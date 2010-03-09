<import resource="classpath:alfresco/site-webscripts/org/alfresco/components/rules/config/rule-config.lib.js">

function main()
{
   var c = new XML(config.script);
   processScriptConfig(c);

   // Load rule config definitions, or in this case "ActionConditionDefinition:s"
   var actionConditionDefinitions = loadRuleConfigDefinitions(c);
   /**
    * Remove the "compare-property-value" action condition definition from the list
    * and put it as a dedicated variable since that condition is a special case
    * and will be dynamically created on the page based on which property that is selected.
    */
   var i = 0;
   for (; i < actionConditionDefinitions.length; i++)
   {
      if (actionConditionDefinitions[i].name == "compare-property-value")
      {
         model.comparePropertyValueDefinition = jsonUtils.toJSONString(actionConditionDefinitions[i]);
         actionConditionDefinitions.splice(i, 1);
      }
      else if (actionConditionDefinitions[i].name == "compare-mime-type")
      {
         model.compareMimeTypeDefinition = jsonUtils.toJSONString(actionConditionDefinitions[i]);
         actionConditionDefinitions.splice(i, 1);
      }
   }   
   model.ruleConfigDefinitions = jsonUtils.toJSONString(actionConditionDefinitions);

   // Load constraints for rule types
   var conditionConstraints = loadRuleConstraints(c);
   model.constraints = jsonUtils.toJSONString(conditionConstraints);


   // Save property-evaluator config as json
   var propertyEvaluatorMap = {},
      propertyEvaluatorNodes = c.elements("property-evaluators"),
      propertyEvaluatorNode = propertyEvaluatorNodes && propertyEvaluatorNodes.length() > 0 ? propertyEvaluatorNodes[0] : null,
      evaluatorNode,
      propertyNode,
      propertyTypes;
   if (propertyEvaluatorNode)
   {
      for each (evaluatorNode in propertyEvaluatorNode.elements("evaluator"))
      {
         propertyTypes = [];
         for each (propertyNode in evaluatorNode.elements("property"))
         {
            propertyTypes.push(propertyNode.@type.toString());
         }
         propertyEvaluatorMap[evaluatorNode.@name.toString()] = propertyTypes;
      }
   }
   model.propertyEvaluatorMap = jsonUtils.toJSONString(propertyEvaluatorMap);

   // Load user preferences for which proeprties to show in menu as default
   var connector = remote.connect("alfresco");
   var result = connector.get("/api/people/" + user.id + "/preferences?pf=org.alfresco.share.rule.properties");
   if (result.status == 200)
   {
      var prefs = eval('(' + result + ')'),
         ruleProperties = {};
      // Get all default properties
      if (c.defaults)
      {
         for each (propertyNode in c.defaults.elements("property"))
         {
            ruleProperties[propertyNode.text()] = "show";
         }
      }

      // Complete with user preferences
      if (prefs && prefs.org && prefs.org.alfresco && prefs.org.alfresco.share && prefs.org.alfresco.share.rule && prefs.org.alfresco.share.rule.properties)
      {
         var userProperties = prefs.org.alfresco.share.rule.properties;
         for (propertyName in userProperties)
         {
            // Will set value to "show" or "hide"
            ruleProperties[propertyName] = userProperties[propertyName];
         }
      }

      // Get info such as type and display name about the properties to display
      var propertiesParam = [],
         transientPropertyInstructions = {},
         instructions,
         propertyNameTokens,
         basePropertyName;
      for (propertyName in ruleProperties)
      {
         if (ruleProperties[propertyName] == "show")
         {
            propertyNameTokens = propertyName.split(":");
            basePropertyName = propertyNameTokens[0] + ":" + propertyNameTokens[1];
            instructions = transientPropertyInstructions[basePropertyName];
            if (!instructions)
            {
               instructions = [];
               transientPropertyInstructions[basePropertyName] = instructions;
            }
            propertiesParam.push(basePropertyName);
            instructions.push(propertyNameTokens.length > 2 ? propertyName : basePropertyName);
         }
      }

      var allProperties = [],
         instruction;
      if (propertiesParam.length > 0)
      {
         result = connector.get("/api/properties?name=" + propertiesParam.join("&name="));
         if (result.status == 200)
         {
            var properties = eval('(' + result + ')');
            for (var i = 0, il = properties.length; i < il; i++)
            {
               property = properties[i];
               instructions = transientPropertyInstructions[property.name];
               for (var j = 0, jl = instructions ? instructions.length : 0; j < jl; j++)
               {
                  instruction = instructions[j];
                  if (instruction == property.name)
                  {
                     // It was a normal property, just add it
                     allProperties.push(
                     {
                        name: property.name,
                        dataType: property.dataType,
                        title: property.title
                     });
                  }
                  else
                  {
                     // It was a transient property, modify the id to represent the transient property
                     allProperties.push(
                     {
                        name: instruction,
                        dataType: property.dataType,
                        title: null // will be set inside client js file instead
                     });
                  }
               }
            }
         }
      }
      model.properties = jsonUtils.toJSONString(allProperties);
   }
}

main();
