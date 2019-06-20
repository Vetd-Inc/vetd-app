process.env.CHROME_BIN = require('puppeteer').executablePath()
// process.env.CHROME_BIN = "/usr/bin/google-chrome";
//process.env.CHROMIUM_BIN = require('puppeteer').executablePath()

// console.log(require('puppeteer').executablePath());

module.exports = function (config) {
  var root = 'resources/public/karma/test'; // same as :output-dir
  var junitOutputDir = process.env.CIRCLE_TEST_REPORTS || "resources/karma/test/junit";

  config.set({
    frameworks: ['cljs-test'],
    // browsers: ['Chrome_no_sandbox'],
    browsers: ['ChromeHeadless'],

    // you can define custom flags
    customLaunchers: {
      Chrome_no_sandbox: {
        base: 'Chrome',
        flags: ['--disable-gpu --headless --no-sandbox']
      }
      // Chrome_without_security: {
      //   base: 'Chrome',
      //   flags: ['--disable-web-security']
      // }
    },
    
    files: [
      root + '/../test.js', // same as :output-to
      {pattern: root + '/../test.js.map', included: false, watched: false},
      {pattern: root + '/**/*.+(cljs|cljc|clj|js|js.map)', included: false, watched: false}
    ],

    client: {
      args: ['runner.run_karma']
    },

    autoWatchBatchDelay: 500,

    // the default configuration
    junitReporter: {
      outputDir: junitOutputDir + '/karma/example', // results will be saved as $outputDir/$browserName.xml
      outputFile: undefined, // if included, results will be saved as $outputDir/$browserName/$outputFile
      suite: '' // suite will become the package name attribute in xml testsuite element
    }
  })
};
