var ographApp = angular.module('ographApp', [ 'ographControllers',
		'ographServices', 'ographDirectives' ]);

ographApp.config([ '$resourceProvider', function($resourceProvider) {
	// Don't strip trailing slashes from calculated URLs
	$resourceProvider.defaults.stripTrailingSlashes = false;
} ]);