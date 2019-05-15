var exec = require("cordova/exec");

//获取图片数据
exports.getImgData = function(success, error) {
    exec(success, error, "ImgData", "getImgData", []);
};