(function () {
  "use strict";

  var FILTERS = {
    date:     { id: "NATIVE_FILTER-BBuN-l3mWcSzX-b5Acnfd", type: "time" },
    status:   { id: "NATIVE_FILTER-MM7FN0iRy0tIidukPm5vj", type: "select" },
    salary:   { id: "NATIVE_FILTER-eGJjUgTQdyhpPMLQZ4aNU", type: "range" },
    skills:   { id: "NATIVE_FILTER-LvcZFurQZxeKnEWPBUJ0h", type: "select" },
    field:    { id: "NATIVE_FILTER-VeiuwhRJCaYumjc9p9QSd", type: "select" },
    role:     { id: "NATIVE_FILTER-BiGazIXgdWZqlqA_i0zoN", type: "select" },
    exp:      { id: "NATIVE_FILTER-_1arTyLfQNTBI-_7vUy5t", type: "select" },
    format:   { id: "NATIVE_FILTER-_w6EWjY1Kp17PN4nPik0M", type: "select" },
    empl:     { id: "NATIVE_FILTER-lPHHp8yCMSywvn1Zkmpyz", type: "select" },
    employer: { id: "NATIVE_FILTER-eP-yWtKK65GDI53M8m_0c", type: "select" },
    currency: { id: "NATIVE_FILTER-7mfPL28m3N2TFSY_E4Xd2", type: "select" },
    source:   { id: "NATIVE_FILTER-xKWmSPyKblhwXIzZlZEfg", type: "select" }
  };

  var iframe = document.getElementById("dash");
  if (!iframe) return;
  var BASE = iframe.getAttribute("data-base");
  var Q = "'";

  function risonString(s) {
    s = String(s);
    if (s.length && !/^[0-9-]/.test(s) && !/[\s'!:(),*@$]/.test(s)) return s;
    return Q + s.split("!").join("!!").split(Q).join("!" + Q) + Q;
  }
  function risonEncode(v) {
    if (v === null || v === undefined) return "!n";
    if (v === true) return "!t";
    if (v === false) return "!f";
    if (typeof v === "number") return String(v);
    if (typeof v === "string") return risonString(v);
    if (Array.isArray(v)) return "!(" + v.map(risonEncode).join(",") + ")";
    return "(" + Object.keys(v).map(function (k) {
      return risonString(k) + ":" + risonEncode(v[k]);
    }).join(",") + ")";
  }

  function esc(s) {
    return String(s).replace(/%/g, "%25").replace(/;/g, "%3B").replace(/,/g, "%2C").replace(/:/g, "%3A");
  }

  function toCompact(dm) {
    var parts = [];
    Object.keys(FILTERS).forEach(function (code) {
      var f = FILTERS[code];
      var e = dm[f.id];
      if (!e) return;
      var val = (e.filterState || {}).value;
      if (val === null || val === undefined || val === "" || (Array.isArray(val) && !val.length)) return;
      var tok = Array.isArray(val) ? val.map(esc).join(",") : esc(val);
      parts.push(code + ":" + tok);
    });
    return parts.join(";");
  }

  function fromCompact(c) {
    var dm = {};
    if (!c) return dm;
    c.split(";").forEach(function (chunk) {
      if (!chunk) return;
      var i = chunk.indexOf(":");
      if (i < 0) return;
      var f = FILTERS[chunk.slice(0, i)];
      if (!f) return;
      var toks = chunk.slice(i + 1).split(",").map(function (t) {
        try { return decodeURIComponent(t); } catch (e) { return t; }
      });
      var value;
      if (f.type === "time") value = toks.join(",");
      else if (f.type === "range") value = toks.map(function (t) { return t === "" ? null : Number(t); });
      else value = toks;
      dm[f.id] = { id: f.id, filterState: { value: value } };
    });
    return dm;
  }

  function readCompact() {
    var m = (window.location.search || "").match(/[?&]f=([^&]*)/);
    return m ? m[1] : "";
  }

  var compact0 = readCompact();
  var restored = fromCompact(compact0);
  iframe.src = Object.keys(restored).length
    ? BASE + "&native_filters=" + encodeURIComponent(risonEncode(restored))
    : BASE;

  var dashId = null;
  (function resolveDashId() {
    var m = BASE.match(/\/dashboard\/([^/?]+)/);
    if (!m) return;
    fetch("/api/v1/dashboard/" + m[1], { credentials: "same-origin" })
      .then(function (r) { return r.json(); })
      .then(function (j) { dashId = j && j.result && j.result.id; })
      .catch(function () {});
  })();

  function keyOf(search) {
    try { return new URLSearchParams(search).get("native_filters_key"); } catch (e) { return null; }
  }

  var lastKey = "";
  var lastCompact = compact0;
  setInterval(function () {
    var k;
    try { k = keyOf(iframe.contentWindow.location.search); } catch (e) { return; }
    if (!k || k === lastKey) return;
    lastKey = k;
    if (!dashId) return;
    fetch("/api/v1/dashboard/" + dashId + "/filter_state/" + encodeURIComponent(k), { credentials: "same-origin" })
      .then(function (r) { return r.json(); })
      .then(function (j) {
        var raw = j && (j.value !== undefined ? j.value : (j.result && j.result.value));
        if (!raw) return;
        var compact = toCompact(JSON.parse(raw));
        if (compact === lastCompact) return;
        lastCompact = compact;
        window.history.replaceState(null, "", compact ? "/?f=" + compact : "/");
      })
      .catch(function () {});
  }, 800);
})();
