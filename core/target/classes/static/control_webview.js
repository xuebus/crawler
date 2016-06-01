var width = 400;
		var height = 400;
		var dataset = [ 30 , 10 , 43 , 55 , 13 ];

		var svg = d3.select("body")
					.append("svg")
					.attr("width", width)
					.attr("height", height);

		var pie = d3.layout.pie();

		var piedata = pie(dataset);

		var outerRadius = 150;	//外半径
		var innerRadius = 0;	//内半径，为0则中间没有空白

		var arc = d3.svg.arc()	//弧生成器
					.innerRadius(innerRadius)	//设置内半径
					.outerRadius(outerRadius);	//设置外半径

		var color = d3.scale.category10();

		var arcs = svg.selectAll("g")
					  .data(piedata)
					  .enter()
					  .append("g")
					  .attr("transform","translate("+ (width/2) +","+ (width/2) +")");

		arcs.append("path")
			.attr("fill",function(d,i){
				return color(i);
			})
			.attr("d",function(d){
				return arc(d);
			});

		arcs.append("text")
			.attr("transform",function(d){
				return "translate(" + arc.centroid(d) + ")";
			})
			.attr("text-anchor","middle")
			.text(function(d){
				return d.data;
			});

		console.log(dataset);
		console.log(piedata);

/*
var dataset = [ 30 , 10 , 43 , 55 , 13 ];

var pie = d3.layout.pie();
var piedata = pie(dataset);

console.log(piedata)
var outerRadius = 150; //外半径
var innerRadius = 0; //内半径，为0则中间没有空白

var arc = d3.svg.arc()  //弧生成器
    .innerRadius(innerRadius)   //设置内半径
    .outerRadius(outerRadius);  //设置外半径

var arcs = svg.selectAll("g")
    .data(piedata)
    .enter()
    .append("g")
    .attr("transform","translate("+ (width/2) +","+ (width/2) +")");

 arcs.append("path")
     .attr("fill",function(d,i){
         return color(i);
     })
     .attr("d",function(d){
         return arc(d);   //调用弧生成器，得到路径值
     });

var color = d3.scale.category10();   //有十种颜色的颜色比例尺

arcs.append("text")
    .attr("transform",function(d){
        return "translate(" + arc.centroid(d) + ")";
    })
    .attr("text-anchor","middle")
    .text(function(d){
        return d.data;
    });
*/


/*
var color=d3.scale.category10();
var dataset=[5,10,20,45,6,25];
var pie=d3.layout.pie();

var w=300;
var h=300;

var outerRadius=w/2;
var innerRadius=0;
var arc=d3.svg.arc()
	.innerRadius(innerRadius)
	.outerRadius(outerRadius);

var svg=d3.select("d3s")
	.append("svg")
	.attr("width",w)
	.attr("height",h);

var arcs=svg.selectAll("g.arc")
	.data(pie(dataset))
	.enter()
	.append("g")
	.attr("class","arc")
	.attr("transform","translate("+outerRadius+","+outerRadius+")");

arcs.append("path")
	.attr("fill",function(d,i){
		return color(i);
	})
	.attr("d",arc);

arcs.append("text")
	.attr("transform",function(d){
		return "translate("+arc.centroid(d)+")";
	})
	.attr("text-anchor","middle")
	.text(function(d){
		return d.value;
	})*/
