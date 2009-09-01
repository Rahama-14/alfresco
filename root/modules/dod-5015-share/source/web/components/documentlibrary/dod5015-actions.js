/**
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing
 */
 
/**
 * DOD5015 Document Library Actions module
 * 
 * @namespace Alfresco.doclib
 * @class Alfresco.doclib.RecordsActions
 */
(function()
{
   /**
    * Alfresco.doclib namespace
    */
   Alfresco.doclib = Alfresco.doclib || {};
   Alfresco.doclib.RecordsActions = {};
   
   Alfresco.doclib.RecordsActions.prototype =
   {
      /**
       * Accession action.
       *
       * @method onActionAccession
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionAccession: function RDLA_onActionAccession(assets)
      {
         this._dod5015Action("message.accession", assets, "accession");
      },

      /**
       * Accession Complete action.
       *
       * @method onActionAccessionComplete
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionAccessionComplete: function RDLA_onActionAccessionComplete(assets)
      {
         this._dod5015Action("message.accession-complete", assets, "accessionComplete");
      },

      /**
       * Copy single document or folder.
       *
       * @method onActionCopyTo
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionCopyTo: function RDLA_onActionCopyTo(assets)
      {
         this._copyMoveFileTo("copy", assets);
      },

      /**
       * File single document or folder.
       *
       * @method onActionFileTo
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionFileTo: function RDLA_onActionFileTo(assets)
      {
         this._copyMoveFileTo("file", assets);
      },

      /**
       * Move single document or folder.
       *
       * @method onActionMoveTo
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionMoveTo: function RDLA_onActionMoveTo(assets)
      {
         this._copyMoveFileTo("move", assets);
      },
      
      /**
       * Copy/Move/File To implementation.
       *
       * @method _copyMoveFileTo
       * @param mode {String} Operation mode: copy|file|move
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       * @private
       */
      _copyMoveFileTo: function RDLA__copyMoveFileTo(mode, assets)
      {
         // Check mode is an allowed one
         if (!mode in
            {
               copy: true,
               file: true,
               move: true
            })
         {
            throw new Error("'" + mode + "' is not a valid Copy/Move/File to mode.");
         }

         if (!this.modules.copyMoveFileTo)
         {
            this.modules.copyMoveFileTo = new Alfresco.module.RecordsCopyMoveFileTo(this.id + "-copyMoveFileTo");
         }

         this.modules.copyMoveFileTo.setOptions(
         {
            mode: mode,
            siteId: this.options.siteId,
            containerId: this.options.containerId,
            path: this.currentPath,
            files: assets
         }).showDialog();
      },

      /**
       * Close Record Folder action.
       *
       * @method onActionCloseFolder
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionCloseFolder: function RDLA_onActionCloseFolder(assets)
      {
         this._dod5015Action("message.close-folder", assets, "closeRecordFolder");
      },

      /**
       * Cut Off action.
       *
       * @method onActionCutoff
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionCutoff: function RDLA_onActionCutoff(assets)
      {
         this._dod5015Action("message.cutoff", assets, "cutoff");
      },

      /**
       * Declare Record action.
       * Special case handling due to the ability to jump to the Edit Metadata page if the action failed.
       *
       * @method onActionDeclare
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionDeclare: function RDLA_onActionDeclare(assets)
      {
         var displayName = assets.displayName,
            editMetadataUrl = Alfresco.constants.URL_PAGECONTEXT + "site/" + this.options.siteId + "/edit-metadata?nodeRef=" + assets.nodeRef;

         this._dod5015Action("message.declare", assets, "declareRecord", null,
         {
            failure:
            {
               message: null,
               callback:
               {
                  fn: function RDLA_oAD_failure(data)
                  {
                     Alfresco.util.PopupManager.displayPrompt(
                     {
                        title: this.msg("message.declare.failure", displayName),
                        text: this.msg("message.declare.failure.more"),
                        buttons: [
                        {
                           text: this.msg("actions.edit-details"),
                           handler: function RDLA_oAD_failure_editDetails()
                           {
                              window.location = editMetadataUrl;
                              this.destroy();
                           },
                           isDefault: true
                        },
                        {
                           text: this.msg("button.cancel"),
                           handler: function RDLA_oAD_failure_cancel()
                           {
                              this.destroy();
                           }
                        }]
                     });
                  },
                  scope: this
               }
            }
         });
      },

      /**
       * Destroy action.
       *
       * @method onActionDestroy
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionDestroy: function RDLA_onActionDestroy(assets)
      {
         this._dod5015Action("message.destroy", assets, "destroy");
      },

      /**
       * Edit Hold Details action.
       *
       * @method onActionEditHoldDetails
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionEditHoldDetails: function RDLA_onActionEditHoldDetails(assets)
      {
         Alfresco.util.PopupManager.getUserInput(
         {
            title: this.msg("message.edit-hold.title"),
            text: this.msg("message.edit-hold.reason.label"),
            value: assets.dod5015["rma:holdReason"],
            okButtonText: this.msg("button.update"),
            callback:
            {
               fn: function RDLA_onActionEditHoldDetails_callback(value)
               {
                  this._dod5015Action("message.edit-hold", assets, "editHoldReason",
                  {
                     "reason": value
                  });
               },
               scope: this
            }
         });
      },

      /**
       * File Transfer Report action.
       *
       * @method onActionFileTransferReport
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionFileTransferReport: function RDLA_onActionFileTransferReport(assets)
      {
         if (!this.modules.fileTransferReport)
         {
            this.modules.fileTransferReport = new Alfresco.module.RecordsFileTransferReport(this.id + "-fileTransferReport");
         }

         this.modules.fileTransferReport.setOptions(
         {
            siteId: this.options.siteId,
            containerId: this.options.containerId,
            path: this.currentPath,
            files: assets
         }).showDialog();
      },

      /**
       * Freeze action.
       *
       * @method onActionFreeze
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionFreeze: function RDLA_onActionFreeze(assets)
      {
         Alfresco.util.PopupManager.getUserInput(
         {
            title: this.msg("message.freeze.title", assets.length),
            text: this.msg("message.freeze.reason"),
            okButtonText: this.msg("button.freeze.record"),
            callback:
            {
               fn: function RDLA_onActionFreeze_callback(value)
               {
                  this._dod5015Action("message.freeze", assets, "freeze",
                  {
                     "reason": value
                  });
               },
               scope: this
            }
         });
      },

      /**
       * Open Record Folder action.
       *
       * @method onActionOpenFolder
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionOpenFolder: function RDLA_onActionOpenFolder(assets)
      {
         this._dod5015Action("message.open-folder", assets, "openRecordFolder");
      },

      /**
       * Relinquish Hold action.
       *
       * @method onActionRelinquish
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionRelinquish: function RDLA_onActionRelinquish(assets)
      {
         this._dod5015Action("message.relinquish", assets, "relinquishHold");
      },

      /**
       * Reviewed action.
       *
       * @method onActionReviewed
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionReviewed: function RDLA_onActionReviewed(assets)
      {
         this._dod5015Action("message.review", assets, "reviewed");
      },

      /**
       * Split email record action.
       *
       * @method onActionSplitEmail
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionSplitEmail: function RDLA_onActionSplitEmail(assets)
      {
         this._dod5015Action("message.split-email", assets, "splitEmail");
      },

      /**
       * Transfer action.
       *
       * @method onActionTransfer
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionTransfer: function RDLA_onActionTransfer(assets)
      {
         this._dod5015Action("message.transfer", assets, "transfer");
      },

      /**
       * Transfer Complete action.
       *
       * @method onActionTransferComplete
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionTransferComplete: function RDLA_onActionTransferComplete(assets)
      {
         this._dod5015Action("message.transfer-complete", assets, "transferComplete");
      },

      /**
       * Undeclare record.
       *
       * @method onActionUndeclare
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionUndeclare: function RDLA_onActionUndeclare(assets)
      {
         this._dod5015Action("message.undeclare", assets, "undeclareRecord");
      },

      /**
       * Unfreeze record.
       *
       * @method onActionUnfreeze
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       */
      onActionUnfreeze: function RDLA_onActionUnfreeze(assets)
      {
         this._dod5015Action("message.unfreeze", assets, "unfreeze");
      },
      
      /**
       * DOD5015 action.
       *
       * @method _dod5015Action
       * @param i18n {string} Will be appended with ".success" or ".failure" depending on action outcome
       * @param assets {object} Object literal representing one or more file(s) or folder(s) to be actioned
       * @param actionName {string} Name of repository action to run
       * @param actionParams {object} Optional object literal to pass parameters to the action
       * @param configOverride {object} Optional object literal to override default configuration parameters
       * @private
       */
      _dod5015Action: function RDLA__dod5015Action(i18n, assets, actionName, actionParams, configOverride)
      {
         var displayName = "",
            dataObj =
            {
               name: actionName
            };

         if (YAHOO.lang.isArray(assets))
         {
            displayName = assets.length;
            dataObj.nodeRefs = [];
            for (var i = 0, ii = assets.length; i < ii; i++)
            {
               dataObj.nodeRefs.push(assets[i].nodeRef);
            }
         }
         else
         {
            displayName = assets.displayName;
            dataObj.nodeRef = assets.nodeRef;
         }

         if (YAHOO.lang.isObject(actionParams))
         {
            dataObj.params = actionParams;
         }
         
         var config =
         {
            success:
            {
               event:
               {
                  name: "metadataRefresh"
               },
               message: this.msg(i18n + ".success", displayName)
            },
            failure:
            {
               message: this.msg(i18n + ".failure", displayName)
            },
            webscript:
            {
               method: Alfresco.util.Ajax.POST,
               stem: Alfresco.constants.PROXY_URI + "api/rma/actions/",
               name: "ExecutionQueue"
            },
            config:
            {
               requestContentType: Alfresco.util.Ajax.JSON,
               dataObj: dataObj
            }
         };
         
         if (YAHOO.lang.isObject(configOverride))
         {
            config = YAHOO.lang.merge(config, configOverride);
         }

         this.modules.actions.genericAction(config);
      }
   };
})();