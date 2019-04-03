cordova.define("cordova-plugin-samsunghealth.ShealthPlugin", function(require, exports, module) {
function ShealthPlugin() {
}

ShealthPlugin.prototype.getData = function (startTime, endTime, datatypes, successCallback, failureCallback) {
  cordova.exec(successCallback,
               failureCallback,
               "ShealthPlugin",
               "getData",
               [{
                 "startTime" : startTime,
                 "endTime" : endTime,
                 "datatypes": datatypes
               }]);
};
ShealthPlugin.prototype.getSleepData = function (startTime, endTime, datatypes, successCallback, failureCallback) {
  cordova.exec(successCallback,
               failureCallback,
               "ShealthPlugin",
               "getSleepData",
               [{
                 "startTime" : startTime,
                 "endTime" : endTime,
                 "datatypes": datatypes
               }]);
};
ShealthPlugin.prototype.connect = function (reqAuth, successCallback, failureCallback) {
  cordova.exec(successCallback,
               failureCallback,
               "ShealthPlugin",
               "connect",
               [{
                 "reqAuth": reqAuth
               }]);
};

ShealthPlugin.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }

  window.plugins.shealth = new ShealthPlugin();
  return window.plugins.shealth;
};

cordova.addConstructor(ShealthPlugin.install);

});
