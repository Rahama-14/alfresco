/**
 * Admin Console Application Tool component
 */

function main()
{
   model.themes = [];
   
   // retrieve the available theme objects
   var themes = sitedata.getObjects("theme");
   for (var i=0, t; i<themes.length; i++)
   {
      t = themes[i];
      model.themes.push(
      {
         id: t.id,
         title: t.title,
         // current theme ID is in the default model for a script
         selected: (t.id == theme)
      });
   }
}

main();