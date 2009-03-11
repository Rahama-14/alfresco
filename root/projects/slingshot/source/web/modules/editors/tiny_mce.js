/**
 *  Adapter for tinyMCE html editor (http://tinymce.moxiecode.com).
 * 
 */
Alfresco.util.RichEditorManager.addEditor('tinyMCE', function(id,config)
{
   var editor;
   
   return (
   {
      init: function RichEditorManager_tinyMCE_init(id, config)
      {
         config.mode = 'exact';
         config.elements = id;
         config.plugins = (config.plugins && config.plugins != '') ? config.plugins + ', safari': 'safari';
         if (!config.init_instance_callback) 
         {
            config.init_instance_callback = function(o)
            {
               return function(inst)
               {
                  YAHOO.Bubbling.fire("editorInitialized", o);
               };
            }(this);
         }
         editor = new tinymce.Editor(id, config);
         return this;
      },

      getEditor: function RichEditorManager_tinyMCE_getEditor()
      {
         return editor;
      },

      clear: function RichEditorManager_tinyMCE_clear() 
      {
         YAHOO.util.Dom.get(editor.id).value = '';
         editor.setContent('');
      },

      render: function RichEditorManager_tinyMCE_render() 
      {
         editor.render();
      },

      execCommand: 'execCommand',

      disable: function RichEditorManager_tinyMCE_disable()
      {
         editor.hide();
      },

      enable: function RichEditorManager_tinyMCE_enable()
      {
         editor.show();
      },

      getContent: function RichEditorManager_tinyMCE_getContent() 
      { 
         return editor.getContent();
      }, 

      setContent: function RichEditorManager_tinyMCE_setContent(html) 
      { 
         editor.setContent(html);
      }, 

      save: function RichEditorManager_tinyMCE_save()
      {
         editor.save();
      },

      getContainer: function RichEditorManager_tinyMCE_getContainer()
      {
         return editor["editorId"] + "_tbl";
      },
      
      activateButton: function RichEditorManager_tinyMCE_activateButton(buttonId)
      {
         editor.controlManager.setActive(buttonId, true);
      },
      
      deactivateButton: function RichEditorManager_tinyMCE_deactivateButton(buttonId)
      {
         editor.controlManager.setActive(buttonId, false);
      }
   });
});
