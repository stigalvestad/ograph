var ographDirectives = angular.module('ographDirectives', []);

ographDirectives.directive('runnerSearch', [ 'Runner', function(Runner) {

	function link(scope, element, attrs) {

		element.on('$destroy', function() {
			$(element).find('.typeahead').typeahead('destroy');
		});

		scope.runnerSearchResults = [];
		scope.selectedRunners = [];
		var runners = [];

		scope.findRunner = function() {
			scope.pleaseWait = true;
            scope.hasSearched = false;
			runners = Runner.all({
				searchTerm : scope.searchExpression
			}, function() {
                scope.hasSearched = true;
				scope.runnerSearchResults = runners;
			});
		}

		scope.addRunner = function(eventorId) {

			var r = _.find(runners, function(runner) {
				return runner.eventorId == eventorId;
			});
			scope.selectedRunners.push(r);
		}

		scope.removeRunner = function(eventorId) {
			_.remove(scope.selectedRunners, function(runner) {
				return runner.eventorId == eventorId;
			});
		}

		scope.alreadyAdded = function(eventorId) {
			var res = _.find(scope.selectedRunners, function(r) {
				return r.eventorId == eventorId;
			});
			return res != undefined;
		}
	}

	return {
		restrict : 'E',
		link : link,
		templateUrl : 'assets/partials/runner-search.html'
	};
} ]);

ographDirectives.directive('resultsGraph', function() {

	function link(scope, element, attrs) {
		scope.$watch('graphDataWithConfig', function(newDataWithConfig, oldDataWithConfig) {
			var container = $(element).find('.graph-container');
			container.find('.graph-seed').remove();

			if (newDataWithConfig && newDataWithConfig.series && newDataWithConfig.series.length > 0) {
				
				var graphSeed = $('<div class="graph-seed"> </div>').appendTo(container);
				var hcConfigAndData = {
						chart: {
				            type: 'spline',
                            zoomType: 'x'
				        },
				        title: {
				            text: newDataWithConfig.config.title
				        },
				        subtitle: {
				            text: 'Kilde: eventor.no'
				        },
				        xAxis: {
				            type: 'datetime',
                            minRange: 14 * 24 * 3600000, // fourteen days
//				            dateTimeLabelFormats: { // don't display the dummy year
//				                month: '%e. %b',
//				                year: '%b'
//				            },
				            title: {
				                text: 'Dato'
				            }
				        },
				        yAxis: {
				            title: {
				                text: newDataWithConfig.config.title
				            },
				            min: newDataWithConfig.config.yAxis.min,
                            max: newDataWithConfig.config.yAxis.max,
                            startOnTick: false
				        },
				        tooltip: newDataWithConfig.config.tooltip,
				        series: newDataWithConfig.series
				    };
				graphSeed.highcharts(hcConfigAndData);
			}
		}, true);
		
	}
	return {
		restrict : 'E',
		link : link,
		templateUrl : 'assets/partials/results-graph.html'
	};
} );