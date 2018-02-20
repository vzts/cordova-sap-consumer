
var exec = cordova.require('cordova/exec'),
	service = "sapconsumer";

exports.connect = function(win, fail) {
	exec(win, fail, service, "connect", []);
};

exports.sendData = function(handle, data, win, fail) {
	exec(win, fail, service, "sendData", []);
};
