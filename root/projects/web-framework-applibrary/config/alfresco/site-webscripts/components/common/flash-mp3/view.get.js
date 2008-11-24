<import resource="/components/common/js/component.js">

var source = WebStudio.Component.getSource();
if(source == null)
{
	model.ready = false;
	
}
else
{
	// set up the source
	var url = source.downloadURI;
	if(url.substring(0,1) == "/")
	{
		url = url.context + source.proxiedDownloadURI;
	}
	model.src = url;

	model.title = instance.properties["title"];
	if(model.title == null)
	{
		model.title = "";
	}

	model.description = instance.properties["description"];
	if(model.description == null)
	{
		model.description = "";
	}
	
	model.appearance = instance.properties["appearance"];
	if(model.appearance == null)
	{
		model.appearance = "full";
	}

	model.songTitle = instance.properties.songTitle;
	if(model.songTitle == null)
	{
		model.songTitle = source.id;
	}

	model.ready = true;
}