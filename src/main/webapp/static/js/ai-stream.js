/**
 * TA Recruitment — consume SSE from /api/ai/stream (data: JSON with base64 UTF-8 payloads).
 */
(function (global) {
  'use strict';

  function decodeB64Utf8(b64) {
    var bin = atob(b64);
    var bytes = new Uint8Array(bin.length);
    for (var i = 0; i < bin.length; i++) {
      bytes[i] = bin.charCodeAt(i);
    }
    return new TextDecoder().decode(bytes);
  }

  /**
   * @param {string} url
   * @param {{ onMeta?: (m:string)=>void, onDelta?: (d:string)=>void, onDone?: ()=>void, onError?: (e:string)=>void }} handlers
   */
  function consumeAiStream(url, handlers) {
    var settled = false;

    function fail(message) {
      if (settled) return;
      settled = true;
      if (handlers.onError) handlers.onError(message);
    }

    function finish() {
      if (settled) return;
      settled = true;
      if (handlers.onDone) handlers.onDone();
    }

    return fetch(url, { credentials: 'same-origin' }).then(function (res) {
      if (!res.ok) {
        fail('HTTP ' + res.status);
        return;
      }
      if (!res.body || typeof res.body.getReader !== 'function') {
        fail('AI streaming response is unavailable.');
        return;
      }
      var reader = res.body.getReader();
      var dec = new TextDecoder();
      var buf = '';

      function processBlock(block) {
        if (settled) return;
        var lines = block.split('\n');
        var i;
        for (i = 0; i < lines.length; i++) {
          if (settled) return;
          var line = lines[i].trim();
          if (line.indexOf('data:') !== 0) {
            continue;
          }
          var payload = line.slice(line.indexOf(':') + 1).trim();
          var obj;
          try {
            obj = JSON.parse(payload);
          } catch (e) {
            continue;
          }
          if (obj.type === 'meta' && obj.b64 && handlers.onMeta) {
            handlers.onMeta(decodeB64Utf8(obj.b64));
          } else if (obj.type === 'delta' && obj.b64 && handlers.onDelta) {
            handlers.onDelta(decodeB64Utf8(obj.b64));
          } else if (obj.type === 'error') {
            fail(obj.b64 ? decodeB64Utf8(obj.b64) : 'AI request failed.');
          } else if (obj.type === 'done') {
            finish();
          }
        }
      }

      function pump() {
        return reader.read().then(function (result) {
          if (result.done) {
            buf = (buf + dec.decode()).replace(/\r\n/g, '\n');
            if (buf.trim()) processBlock(buf);
            if (!settled) fail('AI stream ended before completion.');
            return;
          }
          buf = (buf + dec.decode(result.value, { stream: true })).replace(/\r\n/g, '\n');
          var idx;
          while ((idx = buf.indexOf('\n\n')) >= 0) {
            var chunk = buf.slice(0, idx);
            buf = buf.slice(idx + 2);
            processBlock(chunk);
          }
          return pump();
        });
      }
      return pump();
    }).catch(function (err) {
      fail(err && err.message ? err.message : String(err));
    });
  }

  function renderMarkdown(el, rawText, markedLib) {
    if (!el) return;
    var md = rawText || '';
    if (markedLib && typeof markedLib.parse === 'function') {
      el.innerHTML = markedLib.parse(md, { mangle: false, headerIds: false });
    } else {
      el.textContent = md;
    }
  }

  global.TaAiStream = {
    consume: consumeAiStream,
    renderMarkdown: renderMarkdown
  };
})(typeof window !== 'undefined' ? window : this);
