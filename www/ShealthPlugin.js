function ShealthPlugin() {
}

ShealthPlugin.prototype.getData = function (startTime, endTime, datatypes, successCallback, failureCallback) {
  cordova.exec(successCallback,
               failureCallback,
               "ShealthPlugin",
               "getData",
               [{
                 "startTime" : startTime
               }]);
};
ShealthPlugin.prototype.connect = function (reqAuth, successCallback, failureCallback) {
  cordova.exec(successCallback,
               failureCallback,
               "ShealthPlugin",
               "connect",
               []);
};

ShealthPlugin.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }

  window.plugins.shealth = new ShealthPlugin();
  return window.plugins.shealth;
};

cordova.addConstructor(ShealthPlugin.install);