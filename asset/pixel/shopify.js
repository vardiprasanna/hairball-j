var pixelId = (function() {
    var scriptObjs = document.getElementsByTagName('script');

    for (var i = 0; i < scriptObjs.length; i++) {
        var src = scriptObjs[i].src;
        var dp = src.match(/\/pixel.js[\/|?].*_dp=(\d+)/i);
        if (dp && dp.length >= 2) {
            return dp[1];
        }
    }
    return null;
})();

if (pixelId) {
    (function (w, d, t, r, u) {
        w[u] = w[u] || [];
        w[u].push({'projectId': '10000', 'properties': {'pixelId': String(pixelId)}});
        var s = d.createElement(t);
        s.src = r;
        s.async = true;
        s.onload = s.onreadystatechange = function () {
            var y, rs = this.readyState, c = w[u];
            if (rs && rs != "complete" && rs != "loaded") {
                return
            }
            try {
                y = YAHOO.ywa.I13N.fireBeacon;
                w[u] = [];
                w[u].push = function (p) {
                    y([p])
                };
                y(c)
            } catch (e) {
            }
        };
        var scr = d.getElementsByTagName(t)[0], par = scr.parentNode;
        par.insertBefore(s, scr)
    })(window, document, "script", "https://s.yimg.com/wi/ytc.js", "dotq");

    (function () {
        var _m = window.meta;
        if (window.ShopifyAnalytics && window.ShopifyAnalytics.meta) {
            _m = window.ShopifyAnalytics.meta;
        }
        if (_m && _m.product) {
            window.dotq = window.dotq || [];
            window.dotq.push({
                'projectId': '10000',
                'properties': {
                    'pixelId': String(pixelId),
                    'qstrings': {
                        'et': 'custom',
                        'ea': 'ViewProduct',
                        'product_id': String(_m.product.id)
                    }
                }
            });
        }
    })(document);
}
