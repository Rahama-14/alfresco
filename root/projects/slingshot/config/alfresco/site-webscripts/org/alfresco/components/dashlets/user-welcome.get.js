function main()
{
	// Call the repo for sites the user is a member of
	var result = remote.call("/api/people/" + user.name + "/sites");
	if (result.status == 200)
	{
		// Create javascript objects from the server response
		var sites = eval('(' + result + ')');
      if(sites.length > 2)
      {
         sites = sites.slice(sites.length - 3);
      }

      // Prepare the model for the template
		model.sites = sites;
	}
}

main();