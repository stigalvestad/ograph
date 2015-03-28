var underscore = angular.module('underscore', []);
underscore.factory('_', function() {
  return window._; // assumes underscore has already been loaded on the page
});
underscore.factory('moment', function() {
    return window.moment;
});
underscore.factory('numeral', function() {
    return window.numeral;
});