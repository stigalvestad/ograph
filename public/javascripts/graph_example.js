var drawGraphFunc = function (idContainer, graphData) {
	var container = $('#' + idContainer);
	$('#graph_seed').remove();
	var graphSeed = $('<div id="graph_seed"> </div>').appendTo(container);
	graphSeed.highcharts({
		chart: {
            type: 'spline'
        },
        title: {
            text: 'Plassering'
        },
        subtitle: {
            text: 'Kilde: eventor.no'
        },
        xAxis: {
            type: 'datetime',
            dateTimeLabelFormats: { // don't display the dummy year
                month: '%e. %b',
                year: '%b'
            },
            title: {
                text: 'Date'
            }
        },
        yAxis: {
            title: {
                text: 'Plassering'
            },
            min: 1
        },
        tooltip: {
            headerFormat: '<b>{series.name}</b><br>',
            pointFormat: '{point.x:%e. %b}: {point.y:.2f} plass'
        },
        series: graphData
    });
};
