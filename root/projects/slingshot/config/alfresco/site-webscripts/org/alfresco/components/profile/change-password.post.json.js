/**
 * User Profile Change Password Update method
 * 
 * @method POST
 */
 
function main()
{
   var oldpass = null;
   var newpass1 = null;
   var newpass2 = null;
   
   var names = json.names();
   for (var i=0; i<names.length(); i++)
   {
      var field = names.get(i);
      
      // look and set simple text input values
      if (field.indexOf("-oldpassword") != -1)
      {
         oldpass = new String(json.get(field));
      }
      else if (field.indexOf("-newpassword1") != -1)
      {
         newpass1 = new String(json.get(field));
      }
      else if (field.indexOf("-newpassword2") != -1)
      {
         newpass2 = new String(json.get(field));
      }
   }
   
   // ensure we have valid values and that the new passwords match
   if (newpass1.equals(newpass2))
   {
      // perform the REST API to change the user password
      var params = new Array(2);
      params["oldpw"] = oldpass;
      params["newpw"] = newpass1;
      var connector = remote.connect("alfresco");
      var result = connector.post(
            "/api/person/changepassword/" + stringUtils.urlEncode(user.name),
            jsonUtils.toJSONString(params),
            "application/json");
      var repoJSON = eval('(' + result + ')');
      if (repoJSON.success !== undefined)
      {
         model.success = repoJSON.success;
      }
      else
      {
         model.success = false;
         model.errormsg = repoJSON.message;
      }
   }
   else
   {
      model.success = false;
   }
}

main();