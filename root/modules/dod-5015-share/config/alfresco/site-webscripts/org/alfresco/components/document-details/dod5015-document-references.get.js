var namesToGet = {
   to:[],
   from:[]
};

function getDocNames(nodeRefs)
{
   //must regexp this
   // nodeRef = nodeRef.replace(':','').replace("\\",'').replace('//','/');
   // var result = remote.call("/slingshot/doclib/dod5015/doclist/node/"+nodeRef);
   
   if (nodeRefs.length>0)
   {
      var docNames = [];
      var connector = remote.connect("alfresco");
      var result = connector.post("/api/forms/picker/items",jsonUtils.toJSONString({"items":nodeRefs}), "application/json");
      model.result = result;
      if (result.status == 200)
      {
         var data = eval('(' + result + ')');
         var items = data.data.items;
         if (items.length>0)
         {
            for (var i=0,len=items.length;i<len;i++)
            {
               docNames.push(items[i].name);
            }
            return docNames;
         }
      }
      return [];
   }
   // if (result.status == 200)
   // {
   //    var data = eval('(' + result + ')');
   //    return data.items[0].displayName;
   // }
   // else return nodeRef;
}
/*
 * Note, "From" is customreferences from this node and *not* from other documents to this node.
 * @returns {Object}
 *  {
 *    toThisNode : [] // array of references to this node
 *    fromThisNode : [] // array of references from this node
 *  }
 */
function getDocReferences()
{
   var nodeRef = page.url.args.nodeRef.replace(":/", "");
   var result = remote.call("/api/node/"+nodeRef+"/customreferences");

   var marshallDocRefs = function marshallDocRefs(docrefs, type)
   {      
      if (type === 'from')
      {
         labelField = 'source';
      }
      else 
      {
         labelField = 'target';
      }
      for (var i=0,len = docrefs.length;i<len;i++)
      {
         var ref = docrefs[i];
         var refField,labelField;
         if (ref.referenceType=='parentchild')
         {
            if (type == 'from')
            {
               refField = 'childRef';
               labelField = 'source';               
            }
            else
            {
               refField = 'parentRef';  
               labelField = 'target';               
            }
         }
         else
         {
            if (type == 'from')
            {
               refField = 'targetRef';
            }
            else
            {
               refField = 'sourceRef';
               ref.targetRef = ref.sourceRef;
            }
         }
         //we have to get document names since this api call doesn't return them;
         //so we collect an array of noderefs and do later on
         namesToGet[type].push(ref[refField]);
         if (ref.referenceType=='parentchild')
         {
            ref.label = ref[labelField];
            ref.targetRef = ref[refField];
         }
         docrefs[i]=ref;
      }
      
      return docrefs;
   };
   if (result.status == 200)
   {
      var data = eval('(' + result + ')');
      var docrefs = {
         toThisNode: marshallDocRefs(data.data.customReferencesTo,'to'),
         fromThisNode: marshallDocRefs(data.data.customReferencesFrom,'from')
      };
      return docrefs;
   }
   else {
      return [];
   }
}

model.references = getDocReferences();
model.docNames = {};
model.docNames.to = getDocNames(namesToGet.to);
model.docNames.from = getDocNames(namesToGet.from);
