module.exports = function (context) {

  var path = context.requireCordovaModule('path'),
    fs = context.requireCordovaModule('fs'),
    crypto = context.requireCordovaModule('crypto'),
    Q = context.requireCordovaModule('q'),
    cordova_util = context.requireCordovaModule('cordova-lib/src/cordova/util'),
    platforms = context.requireCordovaModule('cordova-lib/src/platforms/platforms');

  var deferral = new Q.defer();
  var projectRoot = cordova_util.cdProjectRoot();

  var key = crypto.randomBytes(24).toString('base64');
  var iv = crypto.randomBytes(12).toString('base64');

  console.log('key=' + key + ', iv=' + iv);

  context.opts.platforms.filter(function (platform) {
    var pluginInfo = context.opts.plugin.pluginInfo;
    return pluginInfo.getPlatformsArray().indexOf(platform) > -1;

  }).forEach(function (platform) {
    var platformPath = path.join(projectRoot, 'platforms', platform);
    var platformApi = platforms.getPlatformApi(platform, platformPath);
    var platformInfo = platformApi.getPlatformInfo();

    if (process.env.pemCerts) {
      let certs = process.env.pemCerts.split(',');
      let encryptedPems = certs.map((pemCert) => encryptData(pemCert, key, iv));

      if (platform == 'ios') {
        var pluginDir;

        try {
          var ios_parser = context.requireCordovaModule('cordova-lib/src/cordova/metadata/ios_parser'),
            iosParser = new ios_parser(platformPath);
          pluginDir = path.join(iosParser.cordovaproj, 'Plugins', context.opts.plugin.id);
        } catch (err) {
          var xcodeproj_dir = fs.readdirSync(platformPath).filter(function(e) { return e.match(/\.xcodeproj$/i); })[0],
            xcodeproj = path.join(platformPath, xcodeproj_dir),
            originalName = xcodeproj.substring(xcodeproj.lastIndexOf(path.sep)+1, xcodeproj.indexOf('.xcodeproj')),
            cordovaproj = path.join(platformPath, originalName);

          pluginDir = path.join(cordovaproj, 'Plugins', context.opts.plugin.id);
        }

        replaceCryptKey_ios(pluginDir, key, iv, encryptedPems);

      } else if (platform === 'android') {
        var pluginDir = path.join(platformPath, 'src');
        replaceCryptKey_android(pluginDir, key, iv, encryptedPems);
        console.log(`Config file being altered is ${platformInfo.projectConfig.path}`);
      }


    } else {
      console.warn("WARNING - No PEM Certs defined, ignore this warning if running cordova platfrom add");
    }
  });


  deferral.resolve();
  return deferral.promise;

  function encryptData(input, key, iv) {
    var cipher = crypto.createCipheriv('aes-256-cbc', key, iv);
    var encrypted = cipher.update(input, 'utf8', 'base64') + cipher.final('base64');

    return encrypted;
  }

  function replaceCryptKey_ios(pluginDir, key, iv, encryptedPems) {
    let pemArrString = '';

    if(encryptedPems) {
      var sourceFile = path.join(pluginDir, 'CertificateManager.m');
      var content = fs.readFileSync(sourceFile, 'utf-8');
      console.log(`pem array string is \n${pemArrString}`);
      encryptedPems.forEach((str, index) => {
        console.log(`Adding encrypted PEM at index ${index} to array string - \n${str}`);
        pemArrString += `@"${str}",`;
      });
      pemArrString = pemArrString.slice(0, -1);
    }

    content = content.replace(/NSArray \*array = @\[ @"certificates" ];/, 'NSArray *array = @[' + pemArrString + '];')
      .replace(/NSString \*iv = @"iv";/, 'NSString *iv = @"' + iv + '";')
      .replace(/NSString \*key = @"key";/, 'NSString *key = @"' + key + '";');

    fs.writeFileSync(sourceFile, content, 'utf-8');
  }

  function replaceCryptKey_android(pluginDir, key, iv, encryptedPems) {
    let pemArrString = '';

    if(encryptedPems) {
      var sourceFile = path.join(pluginDir, 'com/synconset/cordovahttp/CordovaHttpPlugin.java');
      var content = fs.readFileSync(sourceFile, 'utf-8');
      console.log(`pem array string is \n${pemArrString}`);
      encryptedPems.forEach((str, index) => {
        console.log(`Adding encrypted PEM at index ${index} to array string - \n${str}`);
        pemArrString += `"${str}",`;
      });
      pemArrString = pemArrString.slice(0, -1);
    }

    console.log(`Array string is ${pemArrString}`);
    content = content.replace(/ck = ".*";/, 'ck = "' + key + '";')
      .replace(/String c4 = ".*";/, 'String c4 = "' + iv + '";')
      .replace(/String\[] in = {.*};/, 'String[] in = {' + pemArrString + '};');

    fs.writeFileSync(sourceFile, content, 'utf-8');
  }
};
