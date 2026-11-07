// Replaces the plain browser alert box with a WorkMatch styled popup
(function () {
    function show(message) {
        var old = document.getElementById('wmAlertOverlay');
        if (old) old.remove();

        var overlay = document.createElement('div');
        overlay.id = 'wmAlertOverlay';
        overlay.style.cssText = 'position:fixed; top:0; left:0; width:100vw; height:100vh; background:rgba(0,0,0,0.45); display:flex; justify-content:center; align-items:center; z-index:99999; font-family:Inter, sans-serif;';

        var card = document.createElement('div');
        card.setAttribute('role', 'alertdialog');
        card.style.cssText = 'background:#ffffff; width:420px; max-width:90%; border-radius:16px; padding:28px; box-shadow:0 20px 25px -5px rgba(0,0,0,0.2); border-top:5px solid #115e59;';

        var title = document.createElement('div');
        title.style.cssText = 'font-size:11px; font-weight:700; color:#115e59; letter-spacing:0.5px; text-transform:uppercase; margin-bottom:10px;';
        title.textContent = 'WorkMatch';

        var text = document.createElement('div');
        text.style.cssText = 'font-size:15px; color:#1a202c; line-height:1.5; white-space:pre-wrap; margin-bottom:22px;';
        text.textContent = message;

        var btn = document.createElement('button');
        btn.type = 'button';
        btn.textContent = 'OK';
        btn.style.cssText = 'float:right; background:#115e59; color:white; border:none; padding:10px 28px; border-radius:8px; font-weight:600; font-size:14px; cursor:pointer;';

        function close() {
            document.removeEventListener('keydown', onKey);
            overlay.remove();
        }
        function onKey(e) {
            if (e.key === 'Escape' || e.key === 'Enter') close();
        }
        btn.onclick = close;
        document.addEventListener('keydown', onKey);

        card.appendChild(title);
        card.appendChild(text);
        card.appendChild(btn);
        card.appendChild(document.createElement('div')).style.clear = 'both';
        overlay.appendChild(card);
        document.body.appendChild(overlay);
        btn.focus();
    }

    window.alert = function (message) {
        if (document.body) show(message);
        else document.addEventListener('DOMContentLoaded', function () { show(message); });
    };
})();