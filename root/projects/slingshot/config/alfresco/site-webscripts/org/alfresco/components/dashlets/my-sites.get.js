function sortByTitle(site1, site2)
{
   return (site1.title > site2.title) ? 1 : (site1.title < site2.title) ? -1 : 0;
}


function main()
{
	// Call the repo for sites the user is a member of
	var result = remote.call("/api/people/" + user.name + "/sites");
	if (result.status == 200)
	{
		// Create javascript objects from the server response
		var sites = eval('(' + result + ')');

      sites.sort(sortByTitle);
      // Prepare the model for the template
		model.sites = sites;
	}
}

main();