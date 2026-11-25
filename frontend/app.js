/* RoadGuard AI – shared frontend logic (login, API calls, uploads, detections, complaints, maps).
   All data lives in the Spring Boot backend (PostgreSQL). This file keeps a copy of the current
   user's data in memory so the page scripts can stay simple. Role checks here only decide what to
   show: the backend checks the JWT and enforces user/admin permissions itself.

   Backend address: http://localhost:8080 by default. To change it, add this BEFORE app.js:
   <script>window.RG_API_BASE = 'https://your-server';</script> */
(function () {
  const ORIGIN = (window.RG_API_BASE || 'http://localhost:8080').replace(/\/+$/, '');
  const API = ORIGIN + '/api';
  const TOKEN_KEY = 'roadguard_token';

  // Remove data saved by the old localStorage-only prototype (it stored plain-text passwords)
  ['roadguard_user', 'roadguard_users', 'roadguard_v2_complaints', 'roadguard_v2_detections', 'roadguard_v2_sessions',
   'roadguard_complaints', 'roadguard_detections', 'roadguard_sessions'
  ].forEach(k => { try { localStorage.removeItem(k); } catch (e) {} });

  const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const fmtDate = iso => new Date(iso + (iso.length === 10 ? 'T00:00:00' : '')).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  const coord = (lat, lng) => (lat == null || lng == null) ? 'Location not available'
    : Math.abs(lat).toFixed(4) + '° ' + (lat >= 0 ? 'N' : 'S') + ', ' + Math.abs(lng).toFixed(4) + '° ' + (lng >= 0 ? 'E' : 'W');
  const mmss = t => { t = Math.round(t); return String(Math.floor(t / 60)).padStart(2, '0') + ':' + String(t % 60).padStart(2, '0'); };
  const STATUS = {
    pending:     { label: 'Pending',     cls: 'bg-yellow-100 text-yellow-700' },
    in_progress: { label: 'In Progress', cls: 'bg-purple-100 text-purple-700' },
    resolved:    { label: 'Resolved',    cls: 'bg-green-100 text-green-700' }
  };
  const RG = { esc, fmtDate, coord, mmss, STATUS, apiBase: API };

  /* ---------- Token + API helper ---------- */
  const getToken = () => { try { return localStorage.getItem(TOKEN_KEY); } catch (e) { return null; } };
  const setToken = t => { try { localStorage.setItem(TOKEN_KEY, t); } catch (e) {} };
  const clearAuth = () => { try { localStorage.removeItem(TOKEN_KEY); } catch (e) {} };
  const decodeJwt = t => {
    try {
      const p = t.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(p), c => c.charCodeAt(0))));
    } catch (e) { return null; }
  };

  /* Calls the backend. Throws Error(message) on failure. A 401 on a logged-in page means the token
     expired or is invalid, so the person is sent back to the login page. */
  async function api(path, opts) {
    opts = opts || {};
    const headers = { 'Content-Type': 'application/json' };
    const token = opts.auth === false ? null : getToken();
    if (token) headers.Authorization = 'Bearer ' + token;
    let res;
    try {
      res = await fetch(API + path, { method: opts.method || 'GET', headers, body: opts.body ? JSON.stringify(opts.body) : undefined });
    } catch (e) {
      throw new Error('Cannot reach the server. Make sure the backend is running at ' + ORIGIN + '.');
    }
    const text = await res.text();
    let data = null;
    if (text) { try { data = JSON.parse(text); } catch (e) { data = null; } }
    if (!res.ok) {
      if (res.status === 401 && token) { clearAuth(); location.replace('login.html'); }
      const err = new Error((data && data.message) || 'Request failed (' + res.status + ').');
      err.status = res.status;
      throw err;
    }
    return data;
  }
  RG.api = api;

  /* ---------- Auth ---------- */
  // The logged-in person comes from the JWT (name, email, role). Expired tokens count as logged out.
  RG.currentUser = () => {
    const t = getToken(); if (!t) return null;
    const p = decodeJwt(t);
    if (!p || !p.sub || (p.exp && p.exp * 1000 <= Date.now())) { clearAuth(); return null; }
    return { name: p.name || p.sub, email: p.sub, role: String(p.role || 'USER').toLowerCase() };
  };
  RG.login = async function (email, password, role) {
    try {
      const r = await api('/auth/login', { method: 'POST', auth: false, body: { email: (email || '').trim(), password: password, role: role } });
      setToken(r.token);
      return { ok: true, user: RG.currentUser() };
    } catch (e) { return { ok: false, error: e.message }; }
  };
  RG.register = async function (name, email, password) {   // public sign-up always creates a normal user
    try {
      const r = await api('/auth/register', { method: 'POST', auth: false, body: { name: name.trim(), email: email.trim(), password: password } });
      setToken(r.token);
      return { ok: true };
    } catch (e) { return { ok: false, error: e.message }; }
  };
  RG.logout = function () { clearAuth(); location.href = 'login.html'; };
  RG.home = role => role === 'admin' ? 'admin.html' : 'dashboard.html';
  RG.requireRole = function (role) {
    const u = RG.currentUser();
    if (!u) { location.replace('login.html'); return null; }
    if (u.role !== role) { location.replace(RG.home(u.role)); return null; }
    return u;
  };

  /* ---------- Data copy for the current page ---------- */
  const store = { sessions: [], detections: [], complaints: [], users: [] };
  const put = (list, item) => { const i = list.findIndex(x => x.id === item.id); if (i >= 0) list[i] = item; else list.push(item); };
  // Frame pictures come as /api/files/... paths; make them full URLs so <img src> works from any page
  const fixFrames = s => { (s.frames || []).forEach(f => { if (f.img && f.img.charAt(0) === '/') f.img = ORIGIN + f.img; }); return s; };

  async function loadData() {
    const u = RG.currentUser();
    if (!u) return;
    if (u.role === 'admin') {
      const r = await Promise.all([api('/admin/complaints'), api('/admin/detections'), api('/admin/uploads'), api('/admin/users')]);
      store.complaints = r[0]; store.detections = r[1]; store.sessions = r[2].map(fixFrames); store.users = r[3];
    } else {
      const r = await Promise.all([api('/complaints'), api('/detections'), api('/uploads')]);
      store.complaints = r[0]; store.detections = r[1]; store.sessions = r[2].map(fixFrames); store.users = [];
    }
  }
  /* Resolves when the data is loaded. Page scripts do: RG.ready.then(() => render()).
     It always resolves (even if the server is down) so a page can still draw; the error is shown as a toast. */
  RG.ready = loadData().catch(e => {
    RG.loadError = e;
    const show = () => RG.toast(e.message, false);
    if (document.body) show(); else document.addEventListener('DOMContentLoaded', show);
  });

  RG.allUsers = () => store.users;   // admin only (empty for normal users)

  /* ---------- Sessions (uploads) & detections ---------- */
  const mine = list => { const u = RG.currentUser(); return list.filter(x => u && x.owner === u.email); };
  RG.allSessions = () => store.sessions;                              // newest first
  RG.mySessions = () => mine(RG.allSessions());
  RG.session = id => RG.allSessions().find(s => String(s.id) === String(id)) || null;
  RG.allDetections = () => store.detections;
  RG.myDetections = sessionId => mine(RG.allDetections()).filter(d => !sessionId || String(d.sessionId) === String(sessionId));
  RG.detection = id => RG.allDetections().find(d => d.id === id) || null;

  /* Confirm / reject a detection. Resolves true when the server accepted it, false (after a toast) otherwise. */
  RG.setDetectionStatus = async function (id, status) {
    try {
      put(store.detections, await api('/detections/' + id + '/status', { method: 'PATCH', body: { status: status } }));
      return true;
    } catch (e) { RG.toast(e.message, false); return false; }
  };

  /* Send an upload to the backend. It stores the frame pictures, runs detection and returns the session.
     meta: { name, mediaType:'video'|'photo', fileName, gps, frames:[{img,t}], track:[{lat,lng}], lat, lng }
     Resolves to the session, or null (the reason is in RG.lastError). */
  RG.createSession = async function (meta) {
    RG.lastError = null;
    let track = meta.track || [];
    if (track.length > 200) {                 // the server only needs a rough track
      const step = track.length / 200;
      track = Array.from({ length: 200 }, (_, i) => track[Math.floor(i * step)]);
    }
    try {
      const r = await api('/uploads', { method: 'POST', body: {
        name: meta.name, mediaType: meta.mediaType, fileName: meta.fileName, gpsFileName: meta.gps || null,
        lat: meta.lat, lng: meta.lng, track: track, frames: (meta.frames || []).map(f => ({ img: f.img, t: f.t }))
      } });
      const session = fixFrames(r.upload);
      store.sessions.unshift(session);
      r.detections.forEach(d => put(store.detections, d));
      return session;
    } catch (e) { RG.lastError = e.message; return null; }
  };

  /* ---------- Frame pictures ---------- */
  // Illustrated road, used only when no real picture exists.
  function sampleSvg(d) {
    const b = d.bx, cx = (b.x + b.w / 2) * 6.4, cy = (b.y + b.h / 2) * 3.6, rx = b.w * 6.4 / 2, ry = b.h * 3.6 / 2;
    const a = Math.max(0.12, (d.conf - 40) / 60);
    let spots = '';
    for (let i = 0; i < 40; i++) spots += `<circle cx="${140 + ((d.id * 37 * (i + 3) * 53) % 360)}" cy="${150 + ((d.id * 37 * (i + 5) * 29) % 200)}" r="${1 + (i % 3)}" fill="#3a3f47" opacity=".5"/>`;
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 360">
      <defs><linearGradient id="s" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8fb4d9"/><stop offset="1" stop-color="#d9e4ee"/></linearGradient>
      <linearGradient id="r" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#5b6068"/><stop offset="1" stop-color="#2f3338"/></linearGradient>
      <radialGradient id="p"><stop offset="0" stop-color="#0d0f12"/><stop offset=".75" stop-color="#1c1f24"/><stop offset="1" stop-color="#3a3f47"/></radialGradient></defs>
      <rect width="640" height="360" fill="url(#s)"/><rect y="120" width="640" height="240" fill="#6f8a5a"/>
      <polygon points="260,140 380,140 640,360 0,360" fill="url(#r)"/><polygon points="316,140 324,140 340,360 300,360" fill="#e8e1b0" opacity=".85"/>
      ${spots}<ellipse cx="${cx}" cy="${cy}" rx="${rx * 0.85}" ry="${ry * 0.8}" fill="url(#p)" opacity="${a}"/></svg>`;
    return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
  }
  RG.frameOf = function (d) {
    const s = RG.session(d.sessionId);
    return s && d.fi >= 0 && s.frames[d.fi] ? s.frames[d.fi] : null;
  };
  RG.frameLabel = d => d.kind === 'photo' ? 'Photo' : 'Frame #' + d.frame;
  RG.frameTime = d => d.time || (d.kind === 'photo' ? 'Photo' : '—');
  /* Picture + AI box. Put inside a `relative` container. */
  RG.frameHtml = function (d, big) {
    const f = RG.frameOf(d), src = f ? f.img : sampleSvg(d), b = d.bx;
    const col = d.conf >= 85 ? 'border-red-500 bg-red-500/10' : 'border-yellow-400 bg-yellow-400/10';
    return `<img src="${src}" alt="${esc(RG.frameLabel(d))}" class="absolute inset-0 w-full h-full object-cover" draggable="false">
      <div class="absolute border-2 rounded ${col}" style="left:${b.x}%;top:${b.y}%;width:${b.w}%;height:${b.h}%">
        ${big ? `<span class="absolute -top-6 left-0 text-xs bg-black/70 text-white px-1.5 py-0.5 rounded whitespace-nowrap">pothole ${d.conf}%</span>` : ''}
      </div>`;
  };

  /* ---------- Complaints ---------- */
  RG.allComplaints = () => store.complaints;
  RG.myComplaints = () => mine(RG.allComplaints().map(c => Object.assign({ owner: c.email }, c)));
  RG.complaintForDetection = id => RG.allComplaints().find(c => (c.detectionIds || []).indexOf(id) >= 0) || null;
  /* patch = { status } (administrator only) or { description } (owner, while still pending). Resolves true/false. */
  RG.updateComplaint = async function (id, patch) {
    try {
      const c = patch.status
        ? await api('/admin/complaints/' + encodeURIComponent(id) + '/status', { method: 'PATCH', body: { status: patch.status } })
        : await api('/complaints/' + encodeURIComponent(id), { method: 'PATCH', body: { description: patch.description } });
      put(store.complaints, c);
      return true;
    } catch (e) { RG.toast(e.message, false); return false; }
  };
  /* data: { detectionIds, description, severity }. Resolves to the new complaint or null (after a toast). */
  RG.createComplaint = async function (data) {
    try {
      const c = await api('/complaints', { method: 'POST', body: { detectionIds: data.detectionIds, description: data.description, severity: data.severity } });
      store.complaints.unshift(c);
      return c;
    } catch (e) { RG.toast(e.message, false); return null; }
  };
  RG.eligibleDetections = () => RG.myDetections().filter(d => d.status === 'confirmed' && !RG.complaintForDetection(d.id));

  /* ---------- Toast ---------- */
  RG.toast = function (msg, ok) {
    const t = document.createElement('div');
    t.className = 'fixed bottom-6 left-1/2 -translate-x-1/2 z-[100] px-5 py-3 rounded-lg shadow-lg text-sm font-medium text-white ' + (ok === false ? 'bg-red-600' : 'bg-slate-900');
    t.textContent = msg; document.body.appendChild(t);
    setTimeout(() => t.remove(), 3500);
  };

  /* ---------- ONE complaint form for one or many selected frames ---------- */
  RG.openComplaintModal = function (opts) {
    opts = opts || {};
    const eligible = RG.eligibleDetections();
    if (!eligible.length) { RG.toast('No confirmed frames available. Confirm a detection first.', false); return; }
    const pre = new Set(opts.preselect || []);
    const wrap = document.createElement('div');
    wrap.className = 'fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4';
    wrap.innerHTML = `
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-xl p-6 text-slate-800 max-h-[92vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-bold">Create Complaint</h2>
          <button type="button" data-close class="text-slate-400 hover:text-slate-600 text-2xl leading-none">&times;</button>
        </div>
        <form class="space-y-4">
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="block text-sm font-medium text-slate-700">Frames included in this complaint</label>
              <span data-count class="text-xs text-slate-500"></span>
            </div>
            <div class="space-y-2 max-h-56 overflow-y-auto pr-1">
              ${eligible.map(d => { const s = RG.session(d.sessionId);
                return `<label class="flex items-center gap-3 p-2 border border-slate-200 rounded-lg cursor-pointer hover:bg-slate-50">
                  <input type="checkbox" value="${d.id}" ${pre.has(d.id) ? 'checked' : ''} class="w-4 h-4 shrink-0">
                  <div class="relative w-24 aspect-video rounded overflow-hidden bg-slate-200 shrink-0">${RG.frameHtml(d)}</div>
                  <div class="text-sm min-w-0"><div class="font-medium truncate">Pothole #${d.n} — ${esc(s ? s.name : '')}</div>
                  <div class="text-xs text-slate-500">${esc(RG.frameLabel(d))} • ${d.conf}% • ${coord(d.lat, d.lng)}</div></div></label>`; }).join('')}
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">Description</label>
            <textarea name="desc" required rows="3" class="w-full px-3 py-2.5 border border-slate-300 rounded-lg outline-none focus:ring-2 focus:ring-blue-500 resize-none"></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">Severity</label>
            <div class="flex gap-4 text-sm">
              <label class="flex items-center gap-2 cursor-pointer"><input type="radio" name="sev" value="low"> Low</label>
              <label class="flex items-center gap-2 cursor-pointer"><input type="radio" name="sev" value="medium" checked> Medium</label>
              <label class="flex items-center gap-2 cursor-pointer"><input type="radio" name="sev" value="high"> High</label>
            </div>
          </div>
          <div class="flex gap-3 pt-2">
            <button type="submit" class="flex-1 bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg font-medium">Submit Complaint</button>
            <button type="button" data-close class="px-5 py-2.5 border border-slate-300 rounded-lg hover:bg-slate-50">Cancel</button>
          </div>
        </form>
      </div>`;
    document.body.appendChild(wrap);
    const form = wrap.querySelector('form'), boxes = [...wrap.querySelectorAll('input[type=checkbox]')];
    let dirty = false;
    const chosen = () => boxes.filter(b => b.checked).map(b => eligible.find(d => d.id === +b.value));
    const autoDesc = list => {
      if (!list.length) return '';
      const loc = list.find(d => d.lat != null);
      const where = loc ? ' near ' + coord(loc.lat, loc.lng) : '';
      return list.length === 1
        ? `Pothole detected${where} (${list[0].conf}% AI confidence) and confirmed by the reporter. It is a risk to vehicles and two-wheelers.`
        : `${list.length} potholes detected along this stretch${where}, confirmed by the reporter. They are a risk to vehicles and two-wheelers.`;
    };
    const refresh = () => {
      const list = chosen();
      wrap.querySelector('[data-count]').textContent = list.length + ' selected';
      if (!dirty) form.desc.value = autoDesc(list);
    };
    boxes.forEach(b => b.addEventListener('change', refresh));
    form.desc.addEventListener('input', () => { dirty = true; });
    refresh();
    const close = () => wrap.remove();
    wrap.querySelectorAll('[data-close]').forEach(b => b.addEventListener('click', close));
    wrap.addEventListener('click', e => { if (e.target === wrap) close(); });
    form.addEventListener('submit', async e => {
      e.preventDefault();
      const list = chosen();
      if (!list.length) { RG.toast('Select at least one frame.', false); return; }
      if (list.some(d => RG.complaintForDetection(d.id))) { RG.toast('A selected frame already has a complaint.', false); return; }
      const btn = form.querySelector('button[type=submit]');
      btn.disabled = true;
      const c = await RG.createComplaint({ detectionIds: list.map(d => d.id), description: form.desc.value.trim(), severity: form.sev.value });
      btn.disabled = false;
      if (!c) return;                       // createComplaint already showed the reason
      close();
      RG.toast('Complaint ' + c.id + ' created with ' + list.length + ' frame' + (list.length > 1 ? 's' : ''));
      if (opts.onSaved) opts.onSaved(c);
    });
  };

  /* ---------- View / edit a complaint (user page and admin page) ---------- */
  RG.showComplaint = function (id, opts) {
    opts = opts || {};
    const c = RG.allComplaints().find(x => x.id === id); if (!c) return;
    const dets = (c.detectionIds || []).map(RG.detection).filter(Boolean);
    const wrap = document.createElement('div');
    wrap.className = 'fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4';
    const st = STATUS[c.status];
    wrap.innerHTML = `
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-xl p-6 text-slate-800 max-h-[92vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-bold">${opts.edit ? 'Edit' : 'Complaint'} #${esc(c.id)}</h2>
          <button type="button" data-close class="text-slate-400 hover:text-slate-600 text-2xl leading-none">&times;</button>
        </div>
        <dl class="text-sm space-y-2 mb-4">
          ${[['Status', `<span class="text-xs px-2 py-0.5 rounded-full ${st.cls} font-medium">${st.label}</span>`],
             ...(opts.admin ? [['Filed by', esc(c.userName) + ' (' + esc(c.email) + ')']] : []),
             ['Severity', esc(c.severity ? c.severity[0].toUpperCase() + c.severity.slice(1) : '—')],
             ['Location', coord(c.lat, c.lng)], ['Created', fmtDate(c.createdAt)], ['Potholes', String(c.evidence || dets.length)]]
            .map(r => `<div class="flex justify-between gap-4"><dt class="text-slate-500">${r[0]}</dt><dd class="font-medium text-right">${r[1]}</dd></div>`).join('')}
        </dl>
        ${dets.length ? `<div class="grid grid-cols-3 gap-2 mb-4">${dets.map(d => `<div><div class="relative aspect-video rounded overflow-hidden bg-slate-200">${RG.frameHtml(d)}</div><div class="text-[11px] text-slate-500 mt-0.5">${esc(RG.frameLabel(d))} • ${d.conf}%</div></div>`).join('')}</div>` : ''}
        <label class="block text-sm font-medium text-slate-700 mb-1">Description</label>
        <textarea rows="4" ${opts.edit ? '' : 'readonly'} class="w-full px-3 py-2.5 border border-slate-300 rounded-lg outline-none focus:ring-2 focus:ring-blue-500 resize-none">${esc(c.description)}</textarea>
        <div class="flex gap-3 pt-4">
          ${opts.edit ? '<button type="button" data-save class="flex-1 bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg font-medium">Save changes</button>' : ''}
          <button type="button" data-close class="px-5 py-2.5 border border-slate-300 rounded-lg hover:bg-slate-50 ${opts.edit ? '' : 'flex-1'}">Close</button>
        </div>
      </div>`;
    document.body.appendChild(wrap);
    const close = () => wrap.remove();
    wrap.querySelectorAll('[data-close]').forEach(b => b.addEventListener('click', close));
    wrap.addEventListener('click', e => { if (e.target === wrap) close(); });
    const save = wrap.querySelector('[data-save]');
    if (save) save.addEventListener('click', async () => {
      const v = wrap.querySelector('textarea').value.trim();
      if (!v) { RG.toast('Description cannot be empty.', false); return; }
      save.disabled = true;
      const ok = await RG.updateComplaint(id, { description: v });
      save.disabled = false;
      if (!ok) return;
      close(); RG.toast('Complaint updated.');
      if (opts.onSaved) opts.onSaved();
    });
  };

  /* ---------- Maps: Esri (ArcGIS) + CARTO tiles, no API key needed ---------- */
  RG.addBaseMaps = function (map, switcher) {
    const esri = (svc, label) => L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/' + svc + '/MapServer/tile/{z}/{y}/{x}',
      { maxZoom: 19, attribution: 'Tiles &copy; Esri &mdash; ' + label });
    const streets = esri('World_Street_Map', 'Esri, HERE, Garmin, OpenStreetMap contributors');
    streets.addTo(map);
    if (switcher !== false) {
      L.control.layers({
        'Esri Streets': streets,
        'Esri Satellite': esri('World_Imagery', 'Esri, Maxar, Earthstar Geographics'),
        'CARTO Light': L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
          { subdomains: 'abcd', maxZoom: 19, attribution: '&copy; OpenStreetMap contributors &copy; CARTO' })
      }, null, { position: 'topright' }).addTo(map);
    }
    return streets;
  };

  window.RG = RG;
})();
