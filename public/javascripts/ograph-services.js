var ographServices = angular.module('ographServices', [ 'ngResource', 'underscore' ]);

ographServices.factory('Runner', [ '$resource', function($resource) {
	var _5sec = 5000;
	return $resource('runners/:runnerId', {}, {
		results: {method: 'GET', url: 'runners/:runnerId/results', isArray: true, timeout: _5sec}, 
		status: {method: 'GET', url: 'runners/:runnerId/status', isArray: true, timeout: _5sec}, //TODO currently not using this one
		races: {method: 'GET', url: 'runners/:runnerId/races', isArray: true, timeout: _5sec * 5},
		all: {method: 'GET', url: 'runners/', isArray: true, timeout: _5sec}
	});
} ]);

ographServices.factory('Race', [ '$resource', function($resource) {
	var _5sec = 5000;
	return $resource('races/:raceId', {}, {
		update: {method: 'POST', url: 'job/:jobId/races/:raceId/update', isArray: true, timeout: _5sec * 12,
            params: {raceId: '@raceId', jobId: '@jobId'}},
        jobStatus: {method: 'GET', url: 'job/:jobId/status', isArray: true, timeout: _5sec}
    });
} ]);

ographServices.service('UtilMethods', ['_', 'moment', function(_, moment){
    this.makeTooltipContent = function(printYValFn, resultsPerRunner, selectedPoint){
        var timeOfRace = selectedPoint.x;
        var resultsAtTime = _.chain(resultsPerRunner).map(function(resForRunner){
            return _.find(resForRunner.results, function(res){
                return res.raceClass.race.raceDate == timeOfRace
                    && res.runner.eventorId == selectedPoint.point.series.userOptions.runnerId;
            });
        }).reject(function(r){return r == undefined;}).value();
        var res = resultsAtTime[0];
        var fullName = res.runner.firstName + ' '+res.runner.lastName;
        var resLine = '<br/>'+ printYValFn(selectedPoint.y) +', '+ fullName +', ' + res.raceClass.name;
        var niceTime = moment(timeOfRace).format('D/M-YY');
        return '<strong>'+ res.raceClass.race.name +' '+niceTime+'</strong>' + resLine;
    };

    this.guid = function guid() {
        function s4() {
            return Math.floor((1 + Math.random()) * 0x10000)
                .toString(16)
                .substring(1);
        }
        return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
            s4() + '-' + s4() + s4() + s4();
    };
}]);