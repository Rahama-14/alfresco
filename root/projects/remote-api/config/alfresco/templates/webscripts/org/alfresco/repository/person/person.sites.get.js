function main()
{
   // Get the user name of the person to get
   var userName = url.templateArgs.userid;
   
   // Get the person who has that user name
   var person = people.getPerson(userName);
   
   if (person === null)  
   {
      // Return 404 - Not Found      
      status.setCode(status.STATUS_NOT_FOUND, "Person " + userName + " does not exist");
      return;
   }

   // Get the list of sites
   var sites = siteService.listUserSites(userName);
   
   var sizeString = args["size"];
   if (sizeString != null)
	{
		var size = parseInt(sizeString);
		
		if (size != NaN && size < sites.length)
		{
			// TODO this is a tempory implementaion to support preview client
			// Only return the first n sites based on the passed page size
			var pagedSites = Array();
			for (var index = 0; index < size; index++)
			{
				pagedSites[index] = sites[index];	
			}
			
			sites = pagedSites;
		}
	}

   // Pass the queried sites to the template
   model.sites = sites;
}

main();