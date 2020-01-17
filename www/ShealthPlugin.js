function ShealthPlugin() {
}

ShealthPlugin.prototype.getDailySteps = function (startTime, successCallback, failureCallback) {
    cordova.exec(successCallback,
        failureCallback,
        "ShealthPlugin",
        "getDailySteps",
        [{
            "startTime" : startTime
        }]);
};
ShealthPlugin.prototype.getExercises = function (startTime, successCallback, failureCallback) {
    cordova.exec(successCallback,
        failureCallback,
        "ShealthPlugin",
        "getExercises",
        [{
            "startTime" : startTime
        }]);
};
ShealthPlugin.prototype.connect = function (successCallback, failureCallback) {
    cordova.exec(successCallback,
        failureCallback,
        "ShealthPlugin",
        "connect",
        []);
};
ShealthPlugin.prototype.isConnected = function (successCallback, failureCallback) {
    cordova.exec(successCallback,
        failureCallback,
        "ShealthPlugin",
        "isConnected",
        []);
};
ShealthPlugin.prototype.initialize = function (successCallback, failureCallback) {
    cordova.exec(successCallback,
        failureCallback,
        "ShealthPlugin",
        "initialize",
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