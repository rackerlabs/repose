var express = require('express');
var app = express();

var svr = app.listen(8000, function () {
    var host = svr.address().address;
    var port = svr.address().port;

    console.log('Listening at http://%s:%s', host, port);
});

app.all(/^\/.*/, function (req, res) {
    var headerAccept = req.get('accept');
    var deviceId = req.get('X-DEVICE-ID');
    if (deviceId == undefined) {
        deviceId = 'UNDEFINED'
    }
    if (headerAccept == undefined || headerAccept.indexOf('xml') < 0) {
        console.log('JSON: deviceId=%s', deviceId);
        res.set('Content-Type', 'application/json');
        res.status(200).json({"id": deviceId});
    } else {
        console.log('XML: deviceId=%s', deviceId);
        res.set('Content-Type', 'application/xml');
        res.status(200).send('<?xml version="1.0" encoding="UTF-8" standalone="yes"?><deviceId>' + deviceId + '</deviceId>');
    }
});
