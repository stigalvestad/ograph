module.exports = function(config) {
	config.set({

		basePath : '../',

		files : [ 'public/bower_components/jquery/jquery.min.js',
				'public/bower_components/angular/angular.js',
				'public/bower_components/angular-route/angular-route.js',
				'public/bower_components/angular-resource/angular-resource.js',
				'public/bower_components/angular-animate/angular-animate.js',
				'public/bower_components/angular-mocks/angular-mocks.js',
				'public/bower_components/lodash/dist/lodash.js',
				'public/bower_components/moment/moment.js',
				'public/bower_components/numeraljs/numeral.js',
				'public/external2/highcharts/js/highcharts.js',
				'public/javascripts/**/*.js',
				'js_test/unit/**/*.js' ],

		autoWatch : true,

		frameworks : [ 'jasmine' ],

		browsers : [ 'Firefox' ],

		plugins : [ 'karma-chrome-launcher', 'karma-firefox-launcher',
				'karma-jasmine' ],

		junitReporter : {
			outputFile : 'test_out/unit.xml',
			suite : 'unit'
		}

	});
};