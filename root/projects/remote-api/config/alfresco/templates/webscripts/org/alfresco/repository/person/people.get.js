// Get the args
var filter = args["filter"];
var maxResults = args["maxResults"];

if (filter)
{
   filter = filter.replace('"', '');
}

// Get the collection of people
var peopleCollection = people.getPeople(filter, maxResults != null ? parseInt(maxResults) : 0);

// Pass the queried sites to the template
model.people = peopleCollection;