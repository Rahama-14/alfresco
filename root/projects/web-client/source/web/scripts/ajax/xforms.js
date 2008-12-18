/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
////////////////////////////////////////////////////////////////////////////////
// XForms user interface
//
// This script communicates with the XFormBean to produce and manage an xform.
//
// This script requires mootools.js, dojo.js, tiny_mce.js, 
// tiny_mce_wcm_extensions.js, and upload_helper.js to be loaded in advance.
////////////////////////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////////////////////
// initialization
//
// Initiliaze dojo requirements, tinymce, and add a hook to load the xform.
////////////////////////////////////////////////////////////////////////////////

djConfig.parseWidgets = false;
dojo.require("dojo.lfx.html");
alfresco.log = alfresco.constants.DEBUG ? log : Class.empty;

////////////////////////////////////////////////////////////////////////////////
// constants
//
// These are the client side declared constants.  Others relating to namespaces
// and the webapp context path are expected to be provided by the jsp including
// this script.
////////////////////////////////////////////////////////////////////////////////
alfresco.xforms.constants.XFORMS_ERROR_DIV_ID = "alfresco-xforms-error";

alfresco.xforms.constants.EXPANDED_IMAGE = new Image();
alfresco.xforms.constants.EXPANDED_IMAGE.src = 
  alfresco.constants.WEBAPP_CONTEXT + "/images/icons/expanded.gif";

alfresco.xforms.constants.COLLAPSED_IMAGE = new Image();
alfresco.xforms.constants.COLLAPSED_IMAGE.src = 
  alfresco.constants.WEBAPP_CONTEXT + "/images/icons/collapsed.gif";

////////////////////////////////////////////////////////////////////////////////
// widgets
////////////////////////////////////////////////////////////////////////////////

/**
 * Base class for all xforms widgets.  Each widget has a set of common properties,
 * particularly a corresponding xforms node, a node within the browser DOM,
 * a parent widget, and state variables.
 */
alfresco.xforms.Widget = new Class({
  initialize: function(xform, xformsNode, domNode) 
  {
    this.xform = xform;
    this.xformsNode = xformsNode;
    this.id = this.xformsNode.getAttribute("id");
    this._modified = false;
    this._valid = true;
    var b = this.xform.getBinding(this.xformsNode);
    if (b)
    {
      alfresco.log("adding " + this.id + " to binding " + b.id);
      b.widgets[this.id] = this;
    }
    else
    {
      alfresco.log("no binding found for " + this.id);
    }
    this.domNode = domNode || new Element("div");
    this.domNode.setAttribute("id", this.id + "-domNode");
    this.domNode.widget = this;
    this.domNode.addClass("xformsItem");
  },

  /////////////////////////////////////////////////////////////////
  // properties
  /////////////////////////////////////////////////////////////////
      
  /** A reference to the xform. */
  xform: null,

  /** The xformsNode managed by this widget. */
  xformsNode: null,
        
  /** The dom node containing the label for this widget. */
  labelNode: null,
               
  /** The parent widget, or null if this is the root widget. */
  parentWidget: null,
                 
  /** The dom node for this widget. */
  domNode: null,
                 
  /** The dom node containing this widget. */
  domContainer: null,

  /** The parent widget which is using this as a composite. */
  _compositeParent: null,

  /////////////////////////////////////////////////////////////////
  // 
  /////////////////////////////////////////////////////////////////

  /** Sets the widget's modified state, as indicated by an XFormsEvent. */
  setModified: function(b)
  {
    if (this._modified != b)
    {
      this._modified = b;
      this._updateDisplay(false);
      if (this.isValidForSubmit())
      {
        this.hideAlert();
      }
    }
  },

  /** Sets the widget's valid state, as indicated by an XFormsEvent */
  setValid: function(b)
  {
    if (this._valid != b)
    {
      this._valid = b;
      this._updateDisplay(false);
      if (this.isValidForSubmit())
      {
        this.hideAlert();
      }
      else
      {
        this.showAlert();
      }
    }
  },

  /** 
   * Heuristic approach to determine if the widget is valid for submit or
   * if it's causing an xforms-error.
   */
  isValidForSubmit: function()
  {
    if (typeof this._valid != "undefined" && !this._valid)
    {
      alfresco.log(this.id + " is invalid");
      return false;
    }
    if (!this._modified && 
        this.isRequired() && 
        this.getInitialValue() == null)
    {
      alfresco.log(this.id + " is unmodified and required and empty");
      return false;
    }
    if (this.isRequired() && this.getValue() == null)
    {
      alfresco.log(this.id + " is required and empty");
      return false;
    }
    alfresco.log(this.id + " is valid: {" +
               "modified: " + this._modified + 
               ", required: " + this.isRequired() +
               ", initial_value: " + this.getInitialValue() +
               ", value: " + this.getValue() + "}");
    return true;
  },

  /** Returns the depth of the widget within the widget heirarchy. */
  getDepth: function()
  {
    var result = 1;
    var p = this.parentWidget;
    while (p)
    {
      result++;
      p = p.parentWidget;
    }
    return result;
  },

  /** Returns the root group element */
  getViewRoot: function()
  {
    var p = this;
    while (p.parentWidget)
    {
      p = p.parentWidget;
    }
    if (! (p instanceof alfresco.xforms.ViewRoot))
    {
      throw new Error("expected root widget " + p + " to be a view root");
    }
    return p;
  },

  /** Returns true if the parent is an ancestor of the given parent */
  isAncestorOf: function(parentWidget)
  {
    var p = this;
    while (p.parentWidget)
    {
      if (p.parentWidget == parentWidget)
      {
        return true;
      }
      p = p.parentWidget;
    }
    return false;
  },

  /** Sets the widget's enabled state, as indicated by an XFormsEvent */
  setEnabled: function(enabled)
  {
  },

  /** Returns the widget's enabled state */
  isEnabled: function()
  {
    return true;
  },

  /** Sets the widget's required state, as indicated by an XFormsEvent */
  setRequired: function(b)
  {
    if (this._required != b)
    {
      this._required = b;
      this._updateDisplay(false);
    }
  },

  /** Indicates if a value is required for the widget. */
  isRequired: function()
  {
    if (typeof this._required != "undefined")
    {
      return this._required;
    }
    var binding = this.xform.getBinding(this.xformsNode);
    return binding && binding.isRequired();
  },

  /** Sets the widget's readonly state, as indicated by an XFormsEvent */
  setReadonly: function(readonly)
  {
    this._readonly = readonly;
  },

  /** Indicates if the widget's value is readonly. */
  isReadonly: function()
  {
    if (typeof this._readonly != "undefined")
    {
      return this._readonly;
    }
    var binding = this.xform.getBinding(this.xformsNode);
    return binding && binding.isReadonly();
  },

  isVisible: function()
  {
    return true;
  },

  /** Commits the changed value to the server */
  _commitValueChange: function(value)
  {
    if (this._compositeParent)
    {
      this._compositeParent._commitValueChange(value);
    }
    else
    {
      this.xform.setXFormsValue(this.id, value || this.getValue());
    }
  },

  /** Sets the value contained by the widget */
  setValue: function(value, forceCommit)
  {
    if (forceCommit)
    {
      this.xform.setXFormsValue(this.id, value);
    }
  },
  
  /** Returns the value contained by the widget, or null if none is set */
  getValue: function()
  {
    return null;
  },

  /** Sets the widget's initial value. */
  setInitialValue: function(value, forceCommit)
  {
    this._initialValue = 
      (typeof value == "string" && value.length == 0 ? null : value);
    if (forceCommit)
    {
      this.xform.setXFormsValue(this.id, value);
    }
  },

  /** 
   * Returns the widget's local value, either with a local variable, or by 
   * looking it up within the model section. 
   */
  getInitialValue: function()
  {
    if (typeof this._initialValue != "undefined")
    {
      return this._initialValue;
    }

    var xpath = this._getXPathInInstanceDocument();
    var d = this.xformsNode.ownerDocument;
    var contextNode = this.xform.getInstance();
    alfresco.log("locating " + xpath + " in " + contextNode.nodeName);
    this._initialValue = _evaluateXPath("/" + xpath, 
                                        this.xform.getInstance(), 
                                        XPathResult.FIRST_ORDERED_NODE_TYPE);
    if (!this._initialValue)
    {
      alfresco.log("unable to resolve xpath  /" + xpath + " for " + this.id);
      this._initialValue = null;
    }
    else
    {
      this._initialValue = (this._initialValue.nodeType == document.ELEMENT_NODE
                            ? (this._initialValue.firstChild 
                               ? this._initialValue.firstChild.nodeValue
                               : null)
                            : this._initialValue.nodeValue);
      if (typeof this._initialValue == "string" && this._initialValue.length == 0)
      {
        this._initialValue = null;
      }
      alfresco.log("resolved xpath " + xpath + " to " + this._initialValue);
    }
    return this._initialValue;
  },

  /** Produces an xpath to the model node within the instance data document. */
  _getXPathInInstanceDocument: function()
  {
    var binding = this.xform.getBinding(this.xformsNode);
    var xpath = '';
    var repeatIndices = this.getRepeatIndices();
    do
    {
      var s = binding.nodeset;
      if (binding.nodeset == '.')
      {
        binding = binding.parentBinding;
      }
      if (binding.nodeset.match(/.+\[.+\]/))
      {
        s = binding.nodeset.replace(/([^\[]+)\[.*/, "$1");
        s += '[' + (repeatIndices.shift().index) + ']';
      }
      xpath = s + (xpath.length != 0 ? '/' + xpath : "");
      binding = binding.parentBinding;
    }
    while (binding);
    return xpath;
  },

  /** Returns a child node by name within the xform. */
  _getChildXFormsNode: function(nodeName)
  {
    var x = _getElementsByTagNameNS(this.xformsNode, 
                                    alfresco.xforms.constants.XFORMS_NS,
                                    alfresco.xforms.constants.XFORMS_PREFIX,
                                    nodeName);
    for (var i = 0; i < x.length; i++)
    {
      if (x[i].parentNode == this.xformsNode)
      {
        return x[i];
      }
    }
    return null;
  },

  /** Returns the widget's label. */
  getLabel: function()
  {
    var node = this._getChildXFormsNode("label");
    var result = node ? node.firstChild.nodeValue : "";
    if (alfresco.constants.DEBUG)
    {
      result += " [" + this.id + "]";
    }
    return result;
  },

  /** Returns the widget's alert text. */
  getAlert: function()
  {
    var node = this._getChildXFormsNode("alert");
    return node ? node.firstChild.nodeValue : "";
  },

  /** Returns the widget's alert text. */
  getHint: function()
  {
    var node = this._getChildXFormsNode("hint");
    return node ? node.firstChild.nodeValue : null;
  },
  
  /** Makes the label red. */
  showAlert: function()
  {
    if (!this.labelNode.hasClass("xformsItemLabelSubmitError"))
    {
      this.labelNode.addClass("xformsItemLabelSubmitError");
    }
  },

  /** Restores the label to its original color. */
  hideAlert: function()
  {
    if (this.labelNode.hasClass("xformsItemLabelSubmitError"))
    {
      this.labelNode.removeClass("xformsItemLabelSubmitError");
    }
  },

  /** Returns the value of the appearance attribute for widget */
  getAppearance: function()
  {
    var result = (this.xformsNode.getAttribute("appearance") ||
                  this.xformsNode.getAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":appearance"));
    return result == null || result.length == 0 ? null : result;
  },

  /** Updates the display of the widget.  This is intended to be overridden. */
  _updateDisplay: function(recursively)
  {
  },

  /** Destroy the widget and any resources no longer needed. */
  _destroy: function()
  {
    alfresco.log("destroying " + this.id);
  },

  /** 
   * Returns an array of RepeatIndexDatas corresponding to all enclosing repeats.
   * The closest repeat will be at index 0.
   */
  getRepeatIndices: function()
  {
    var result = [];
    var w = this;
    while (w.parentWidget)
    {
      if (w.parentWidget instanceof alfresco.xforms.Repeat)
      {
        result.push(new alfresco.xforms.RepeatIndexData(w.parentWidget,
                                                        w.parentWidget.getChildIndex(w) + 1));
      }
      w = w.parentWidget;
    }
    return result;
  },

  /** 
   */
  getParentGroups: function(appearance)
  {
    var result = [];
    var w = this;
    while (w.parentWidget)
    {
      if (w.parentWidget instanceof alfresco.xforms.AbstractGroup)
      {
        if (appearance && w.parentWidget.getAppearance() == appearance)
        {
          result.push(w.parentWidget);
        }
      }
      w = w.parentWidget;
    }
    return result;
  }
});

////////////////////////////////////////////////////////////////////////////////
// widgets for atomic types
////////////////////////////////////////////////////////////////////////////////

/** The file picker widget which handles xforms widget xf:upload. */
alfresco.xforms.FilePicker = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode, params)
  {
    this.parent(xform, xformsNode);
    this._selectableTypes = "selectable_types" in params ? params["selectable_types"].split(",") : null;
    this._filterMimetypes = "filter_mimetypes" in params ? params["filter_mimetypes"].split(",") : [];
    this._folderRestriction = "folder_restriction" in params ? params["folder_restriction"] : null;
    this._configSearchName = "config_search_name" in params ? params["config_search_name"] : null;  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    this.domNode.addClass("xformsFilePicker");
    attach_point.appendChild(this.domNode);
    //XXXarielb support readonly and disabled
    this.widget = new alfresco.FilePickerWidget(this.id,
                                                this.domNode, 
                                                this.getInitialValue(), 
                                                false,
                                                this._filePicker_changeHandler.bindAsEventListener(this),
                                                null /* cancel is ignored */,
                                                this._filePicker_resizeHandler.bindAsEventListener(this),
                                                this._selectableTypes,
                                                this._filterMimetypes,
                                                this._folderRestriction,
                                                this._configSearchName);
    this.widget.render();
  },

  getValue: function()
  {
    return this.widget.getValue();
  },

  setValue: function(value, forceCommit)
  {
    if (!this.widget)
    {
      this.setInitialValue(value, forceCommit);
    }
    else
    {
      this.parent(value, forceCommit);
      this.widget.setValue(value);
    }
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _filePicker_changeHandler: function(fpw)
  {
    this._commitValueChange();
  },

  _filePicker_resizeHandler: function(fpw) 
  { 
    this.domContainer.style.height = 
      Math.max(fpw.node.offsetHeight + 
               this.domNode.parentNode.getStyle("margin-top").toInt() +
               this.domNode.parentNode.getStyle("margin-bottom").toInt(),
               20) + "px";
  }
});

/** The textfield widget which handle xforms widget xf:input with any string or numerical type */
alfresco.xforms.TextField = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode, new Element("input", { type: "text" }));
    this._maxLength = (_hasAttribute(this.xformsNode, alfresco.xforms.constants.ALFRESCO_PREFIX + ":maxLength")
                       ? Number(this.xformsNode.getAttribute(alfresco.xforms.constants.ALFRESCO_PREFIX + ":maxLength"))
                       : -1);
    this._length = (_hasAttribute(this.xformsNode, alfresco.xforms.constants.ALFRESCO_PREFIX + ":length")
                    ? Number(this.xformsNode.getAttribute(alfresco.xforms.constants.ALFRESCO_PREFIX + ":length"))
                    : -1);

  },
 
  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    var initial_value = this.getInitialValue() || "";
    attach_point.appendChild(this.domNode);

    this.widget = this.domNode;
    this.widget.setAttribute("value", initial_value);
    if (this._maxLength >= 0)
    {
      this.widget.setAttribute("maxlength", this._maxLength);
    }
    
    if (this._length >= 0)
    {
      this.widget.style.maxWidth = "100%";
      this.widget.setAttribute("size", this._length);
    }
    else if (this.getAppearance() == "full")
    {
      var borderWidth = (this.widget.offsetWidth - this.widget.clientWidth);
      var marginRight = 2;
      this.widget.style.marginRight = marginRight + "px";
      this.widget.style.width = (((attach_point.offsetWidth - borderWidth - marginRight) / attach_point.offsetWidth) * 100) + "%";
      this.widget.style.minWidth = "50px";
    }

    if (this.isReadonly())
    {
      this.widget.setAttribute("readonly", this.isReadonly());
      this.widget.setAttribute("disabled", this.isReadonly());
    }
    else
    {
      this.widget.onblur = this._widget_changeHandler.bindAsEventListener(this);
    }
  },

  setValue: function(value, forceCommit)
  {
    if (!this.widget)
    {
      this.setInitialValue(value, forceCommit);
    }
    else
    {
      this.parent(value, forceCommit);
      this.widget.value = value;
    }
  },

  getValue: function()
  {
    return (this.widget.value != null && this.widget.value.length == 0 
            ? null 
            : this.widget.value);
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _widget_changeHandler: function(event)
  {
    this._commitValueChange();
  }
});

/** The number range widget which handle xforms widget xf:range with any numerical type */
alfresco.xforms.NumericalRange = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode);
    dojo.require("dojo.widget.Slider");
    this._fractionDigits = (_hasAttribute(this.xformsNode, alfresco.xforms.constants.ALFRESCO_PREFIX + ":fractionDigits")
                          ? Number(this.xformsNode.getAttribute(alfresco.xforms.constants.ALFRESCO_PREFIX + ":fractionDigits"))
                            : -1);
  },
  
  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    var initial_value = this.getInitialValue() || "";
    attach_point.appendChild(this.domNode);
    var sliderDiv = document.createElement("div");
    sliderDiv.style.fontWeight = "bold";
    sliderDiv.style.marginBottom = "5px";
    this.domNode.appendChild(sliderDiv);

    var minimum = Number(this.xformsNode.getAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":start"));
    var maximum = Number(this.xformsNode.getAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":end"));
    var snapValues = 0;
    if (this._fractionDigits == 0)
    {
      snapValues = maximum - minimum + 1;
    }
    sliderDiv.appendChild(document.createTextNode(minimum));

    var sliderWidgetDiv = document.createElement("div");
    sliderDiv.appendChild(sliderWidgetDiv);
    this.widget = dojo.widget.createWidget("SliderHorizontal",
                                           {
                                             initialValue: initial_value,
                                             minimumX: minimum,
                                             maximumX: maximum,
                                             showButtons: false,
                                             activeDrag: false,
                                             snapValues: snapValues
                                           },
                                           sliderWidgetDiv);
    sliderDiv.appendChild(document.createTextNode(maximum));
    
    this.currentValueDiv = document.createElement("div");
    this.domNode.appendChild(this.currentValueDiv);
    this.currentValueDiv.appendChild(document.createTextNode("Value: " + initial_value));
     
    dojo.event.connect(this.widget,
                       "onValueChanged", 
                       this,
                       this._hSlider_valueChangedHandler);
  },

  setValue: function(value, forceCommit)
  {
    if (!this.widget)
    {
      this.setInitialValue(value, forceCommit);
    }
    else
    {
      this.parent(value, forceCommit);
      this.widget.setValue(value);
    }
  },

  getValue: function()
  {
    return this.widget.getValue();
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////
    
  _hSlider_valueChangedHandler: function(value)
  {
    if (this._fractionDigits >= 0)
    {
      value = Math.round(value * Math.pow(10, this._fractionDigits)) / Math.pow(10, this._fractionDigits);
    }
    this.currentValueDiv.replaceChild(document.createTextNode("Value: " + value),
                                      this.currentValueDiv.firstChild);
    if (!this.widget._isDragInProgress)
    {
      this._commitValueChange();
    }
  }
});

/** The text area widget handles xforms widget xf:textarea with appearance minimal */
alfresco.xforms.PlainTextEditor = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode)
  {
    this.parent(xform, xformsNode, new Element("textarea"));
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    attach_point.appendChild(this.domNode);
    this.domNode.addClass("xformsTextArea");
    var initialValue = this.getInitialValue() || "";
    this.widget = this.domNode;
    this.widget.appendChild(document.createTextNode(initialValue));
    if (this.isReadonly())
    {
      this.widget.setAttribute("readonly", this.isReadonly());
    }
    var borderWidth = (this.widget.offsetWidth - this.widget.clientWidth);
    var marginRight = 2;
    this.widget.style.marginRight = marginRight + "px";
    this.widget.style.width = (((attach_point.offsetWidth - borderWidth - marginRight) / attach_point.offsetWidth) * 100) + "%";
    this.widget.onchange =this._textarea_changeHandler.bindAsEventListener(this);
  },

  setValue: function(value, forceCommit)
  {
    if (!this.widget)
    {
      this.setInitialValue(value, forceCommit);
    }
    else
    {
      this.parent(value, forceCommit);
      this.widget.value = value;
    }
  },

  getValue: function()
  {
    return this.widget.value;
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _textarea_changeHandler: function(event)
  {
    this._commitValueChange();
  }
});

/** The textfield widget which handle xforms widget xf:textarea. with appearance full or compact */
alfresco.xforms.RichTextEditor = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode, params) 
  {
    this.parent(xform, xformsNode);
    this._focused = false;
    this._params = params;
    this._oldValue = null;
  },

  /////////////////////////////////////////////////////////////////
  // methods & properties
  /////////////////////////////////////////////////////////////////
  
  _removeTinyMCE: function()
  {
    var value = this.getValue(); //tinyMCE.getContent(this.id);
    if (value != this._oldValue)
    {
      alfresco.log("commitValueChange from _removeTinyMCE [" + value + "]");
      this._commitValueChange(value);
      this._oldValue = value;
    }
    tinyMCE.removeMCEControl(this.id);
    this._focused = false;
  },

  _createTinyMCE:function()
  {
    if (alfresco.xforms.RichTextEditor.currentInstance &&
        alfresco.xforms.RichTextEditor.currentInstance != this)
    {
        alfresco.xforms.RichTextEditor.currentInstance._removeTinyMCE();
    }

    alfresco.xforms.RichTextEditor.currentInstance = this;

    for (var i in alfresco.constants.TINY_MCE_DEFAULT_SETTINGS)
    {
      if (!(i in this._params))
      {
        this._params[i] = alfresco.constants.TINY_MCE_DEFAULT_SETTINGS[i];
      }
    }
    for (var i in this._params)
    {
      if (i in tinyMCE.settings)
      {
        alfresco.log("setting tinyMCE.settings[" + i + "] = " + this._params[i]);
        tinyMCE.settings[i] = this._params[i];
      }
    }
    tinyMCE.settings.height = this._params["height"] ? parseInt(this._params["height"]) : -1;
    tinyMCE.settings.auto_focus = this.id;
    tinyMCE.addMCEControl(this.widget, this.id);
    
    tinyMCE.getInstanceById(this.id).getWin().focus();
    var editorDocument = tinyMCE.getInstanceById(this.id).getDoc();
    editorDocument.widget = this;

    tinyMCE.addEvent(editorDocument, 
                     window.ie ? "beforedeactivate" : "blur", 
                     this._tinyMCE_blurHandler);
    tinyMCE.addEvent(editorDocument, "focus", this._tinyMCE_focusHandler);
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    attach_point.appendChild(this.domNode);
    this.domNode.addClass("xformsTextArea");
    if (this._params.height)
    {
      this.domNode.setStyle("height", parseInt(this._params["height"]) + "px");
    }
    this.widget = new Element("div");
    this.domNode.appendChild(this.widget);
    this.widget.addClass("xformsTextArea");
    if (this._params["height"])
    {
      this.widget.setStyle("height", parseInt(this._params["height"]) + "px");
    }
    this.widget.style.border = "2px inset #f0f0f0";
    this.widget.style.marginRight = "2px";
    this.widget.style.overflow = "auto";
    this._oldValue = this.getInitialValue() || "";
    this.widget.innerHTML = this._oldValue;

    $each(this.widget.getElementsByTagName("img"), 
          function(img, index)
          {
            if (img.getAttribute("src") && img.getAttribute("src").match("^/"))
            {
              img.setAttribute("src", alfresco.constants.AVM_WEBAPP_URL + img.getAttribute("src"));
            }
          });
    if (!this.isReadonly())
    {
      this.widget.onmouseover = this._div_mouseoverHandler.bindAsEventListener(this);
    }
  },

  setValue: function(value, forceCommit)
  {
    if (value != this._oldValue || forceCommit)
    {
      if (alfresco.xforms.RichTextEditor.currentInstance == this)
      {
        tinyMCE.selectedInstance = tinyMCE.getInstanceById(this.id);
        try
        {
          tinyMCE.setContent(value);
        }
        catch (e)
        {
          //XXXarielb figure this out - getting intermittent errors in IE.
          alfresco.log(e);
        }
      }
      else
      {
        this.widget.innerHTML = value;
      }
    }
    this.parent(value, forceCommit);
  },

  getValue: function()
  {
    var result = (alfresco.xforms.RichTextEditor.currentInstance == this 
                  ? tinyMCE.getContent(this.id) 
                  : this.widget.innerHTML);
    result = result.replace(new RegExp(alfresco.constants.AVM_WEBAPP_URL, "g"), "");
    return result;
  },

  setReadonly: function(readonly)
  {
    this.parent(readonly);
    if (readonly && alfresco.xforms.RichTextEditor.currentInstance == this)
    {
      this._removeTinyMCE();
    }
  },

  _destroy: function()
  {
    this.parent();
    if (!this.isReadonly())
    {
      alfresco.log("removing mce control " + this.id);
      tinyMCE.removeMCEControl(this.id);
    }
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _tinyMCE_blurHandler: function(event)
  {
    if (event.type == "beforedeactivate")
    {
      event.target = event.srcElement.ownerDocument;
    }
    var widget = event.target.widget;
    var value = widget.getValue();
    if (value != widget._oldValue)
    {
      alfresco.log("commitValueChange from _tinyMCE_blurHandler [" + value + "]");
      widget._commitValueChange(value);
      widget._oldValue = value;
    }
    widget._focused = false;
  },

  _tinyMCE_focusHandler: function(event)
  {
    var widget = event.target.widget;
    var repeatIndices = widget.getRepeatIndices();
    if (repeatIndices.length != 0 && !widget._focused)
    {
      var r = repeatIndices[repeatIndices.length - 1].repeat;
      var p = widget;
      while (p && p.parentWidget != r)
      {
        if (p.parentWidget instanceof alfresco.xforms.Repeat)
        {
          throw new Error("unexpected parent repeat " + p.parentWidget.id);
        }
        p = p.parentWidget;
      }
      if (!p)
      {
        throw new Error("unable to find parent repeat " + r.id +
                        " of " + widget.id);
      }
      repeatIndices[repeatIndices.length - 1].repeat.setFocusedChild(p);
    }
    widget._focused = true;
  },

  _div_mouseoverHandler: function(event)
  {
    if (!this.hoverLayer)
    {
      this.hoverLayer = new Element("div");
      this.hoverLayer.addClass("xformsRichTextEditorHoverLayer");
      this.hoverLayer.setText(alfresco.resources["click_to_edit"]);
    }
    if (this.hoverLayer.parentNode != this.widget)
    {
      this.widget.appendChild(this.hoverLayer);
      
      this.hoverLayer.style.lineHeight = this.hoverLayer.offsetHeight + "px";
      this.hoverLayer.setOpacity(.8);
      this.hoverLayer.onmouseout = this._hoverLayer_mouseoutHandler.bindAsEventListener(this);
      this.hoverLayer.onclick = this._hoverLayer_clickHandler.bindAsEventListener(this);
    }
  },

  _hoverLayer_mouseoutHandler: function(event)
  {
    if (this.hoverLayer.parentNode == this.widget)
    {
      this.hoverLayer.setOpacity(1);
      this.widget.removeChild(this.hoverLayer);
    }
  },

  _hoverLayer_clickHandler: function(event)
  {
    if (this.hoverLayer.parentNode == this.widget)
    {
      this.hoverLayer.setOpacity(1);
      this.widget.removeChild(this.hoverLayer);
      this._createTinyMCE();
    }
  }
});

/** The currently rendered rich text editor instance */
alfresco.xforms.RichTextEditor.currentInstance = null;

/** 
 * Reads the widget configuration to determine which plugins will 
 * be needed by tinymce.  All plugins must be loaded into tinymce at
 * startup so they must be accumulated in advance.
 */
alfresco.xforms.RichTextEditor.determineNecessaryTinyMCEPlugins = function(config)
{
  var result = [];
  for (var widget in config)
  {
    for (var schemaType in config[widget])
    {
      for (var appearance in config[widget][schemaType])
      {
        if (config[widget][schemaType][appearance].className == "alfresco.xforms.RichTextEditor" &&
            config[widget][schemaType][appearance].params &&
            config[widget][schemaType][appearance].params.plugins)
        {
          alfresco.log("found plugins definition " +  config[widget][schemaType][appearance].params.plugins +
                     " for text editor at  config[" + widget + "][" + schemaType + "][" + appearance + "]");
          var plugins = config[widget][schemaType][appearance].params.plugins.split(",");
          for (var p = 0; p < plugins.length; p++)
          {
            if (result.indexOf(plugins[p]) < 0)
            {
              result.push(plugins[p]);
            }
          }
        }
      }
    }
  }
  return result.join(",");
}

/** Base class for all select widgets. */
alfresco.xforms.AbstractSelectWidget = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode, domNode) 
  {
    this.parent(xform, xformsNode, domNode);
  },

  /////////////////////////////////////////////////////////////////
  // methods
  /////////////////////////////////////////////////////////////////

  /**
   * Returns the possible item values for the select control as an array
   * of anonymous objects with properties id, label, value, and valid.
   */
  _getItemValues: function()
  {
    var binding = this.xform.getBinding(this.xformsNode);
    var values = _getElementsByTagNameNS(this.xformsNode, 
                                         alfresco.xforms.constants.XFORMS_NS,
                                         alfresco.xforms.constants.XFORMS_PREFIX, 
                                         "item");
    var result = [];
    for (var i = 0; i < values.length; i++)
    {
      var label = _getElementsByTagNameNS(values[i], 
                                          alfresco.xforms.constants.XFORMS_NS,
                                          alfresco.xforms.constants.XFORMS_PREFIX,
                                          "label")[0];
      label = label.firstChild.nodeValue;
      var value = _getElementsByTagNameNS(values[i], 
                                          alfresco.xforms.constants.XFORMS_NS,
                                          alfresco.xforms.constants.XFORMS_PREFIX, 
                                          "value")[0];
      var valueText = value.firstChild.nodeValue;
      var itemId = value.getAttribute("id");
      var valid = true;
      if (binding.constraint)
      {
        if (!window.ie)
        {
          valid = _evaluateXPath(binding.constraint, value, XPathResult.BOOLEAN_TYPE);
          if (alfresco.constants.DEBUG)
          {
            alfresco.log("evaludated constraint " + binding.constraint + 
                         " on " + value + " to " + valid);
          }
        }
        else 
        {
          valid = !(valueText == label && valueText.match(/^\[.+\]$/));
        }
      }
      result.push({ 
        id: itemId, 
        label: valid ? label : "",
        value: valid ? valueText : "_invalid_value_",
        valid: valid
      });

      if (alfresco.constants.DEBUG)
      {
        alfresco.log("values["+ i + "] = {id: " + result[i].id + 
                   ",label: " + result[i].label + ",value: " + result[i].value + 
                   ",valid: " + result[i].valid + "}");
      }
    }
    return result;
  }
});

/** 
 * Handles xforms widget xf:select.  Produces either a multiselect list or a set of
 * checkboxes depending on the number of inputs.
 */
alfresco.xforms.CheckboxSelect = alfresco.xforms.AbstractSelectWidget.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode);
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    var values = this._getItemValues();
    var initial_value = this.getInitialValue();
    initial_value = initial_value ? initial_value.split(' ') : [];
    this._selectedValues = [];
    this.widget = this.domNode;
    attach_point.appendChild(this.domNode);
    for (var i = 0; i < values.length; i++)
    {
      var checkboxDiv = document.createElement("div");
      checkboxDiv.style.lineHeight = "16px";
      this.widget.appendChild(checkboxDiv);
      
      var checkbox = new Element("input");
      checkbox.setAttribute("id", this.id + "_" + i + "-widget");
      checkbox.setAttribute("name", this.id + "_" + i + "-widget");
      checkbox.setAttribute("type", "checkbox");
      checkbox.setAttribute("value", values[i].value);
      if (initial_value.indexOf(values[i].value) != -1)
      {
        this._selectedValues.push(values[i].value);
        checkbox.checked = true;
      }
      checkboxDiv.appendChild(checkbox);
      checkboxDiv.appendChild(document.createTextNode(values[i].label));
      checkbox.onclick = this._checkbox_clickHandler.bindAsEventListener(this);
    }
  },

  setValue: function(value, forceCommit)
  {
    if (!this.widget)
    {
      this.setInitialValue(value, forceCommit);
    }
    else
    {
      this.parent(value, forceCommit);
      this._selectedValues = value.split(' ');
      var checkboxes = this.widgets.getElementsByTagName("input");
      for (var i = 0; i < checkboxes.length; i++)
      {
        checkboxes[i].checked = 
          this._selectedValues.indexOf(checkboxes[i].getAttribute("value")) != -1;
      }
    }
  },

  getValue: function()
  {
    return this._selectedValues.length == 0 ? null : this._selectedValues.join(" ");
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _checkbox_clickHandler: function(event)
  { 
    this._selectedValues = [];
    var all_checkboxes = this.widget.getElementsByTagName("input");
    for (var i = 0; i < all_checkboxes.length; i++)
    {
      if (all_checkboxes[i] && all_checkboxes[i].checked)
      {
        this._selectedValues.push(all_checkboxes[i].getAttribute("value"));
      }
    }
    this._commitValueChange();
  }
});

/** 
 * Handles xforms widget xf:select.  Produces either a multiselect list or a set of
 * checkboxes depending on the number of inputs.
 */
alfresco.xforms.ListSelect = alfresco.xforms.AbstractSelectWidget.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode, new Element("select"));
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    var values = this._getItemValues();
    var initial_value = this.getInitialValue();
    initial_value = initial_value ? initial_value.split(' ') : [];
    this._selectedValues = [];
    attach_point.appendChild(this.domNode);
    this.widget = this.domNode;
    this.widget.setAttribute("multiple", true);
    attach_point.appendChild(this.widget);
    for (var i = 0; i < values.length; i++)
    {
      var option = document.createElement("option");
      option.appendChild(document.createTextNode(values[i].label));
      option.setAttribute("value", values[i].value);
      if (initial_value.indexOf(values[i].value) != -1)
      {
        this._selectedValues.push(values[i].value);
        option.selected = true;
      }
      this.widget.appendChild(option);
    }
    this.widget.onblur = this._list_changeHandler.bindAsEventListener(this);
  },

  setValue: function(value, forceCommit)
  {
    if (!this.widget)
    {
      this.setInitialValue(value, forceCommit);
    }
    else
    {
      this.parent(value, forceCommit);
      this._selectedValues = value.split(' ');
      var options = this.widgets.getElementsByTagName("option");
      for (var i = 0; i < options.length; i++)
      {
        options[i].selected = 
          this._selectedValues.indexOf(options[i].getAttribute("value")) != -1;
      }
    }
  },

  getValue: function()
  {
    return this._selectedValues.length == 0 ? null : this._selectedValues.join(" ");
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _list_changeHandler: function(event) 
  {
    var target;
    
    if (window.ie) 
    { 
      target = window.event.srcElement;
    } 
    else 
    { 
      target = event.target;
    }

    this._selectedValues = [];
    for (var i = 0; i < target.options.length; i++)
    {
      if (target.options[i].selected)
      {
        this._selectedValues.push(target.options[i].getAttribute("value"));
      }
    }
    this._commitValueChange();
  }
});

/** 
 * Handles xforms widget xf:select1.  Produces either a combobox or a set of
 * radios depending on the number of inputs.
 */
alfresco.xforms.RadioSelect1 = alfresco.xforms.AbstractSelectWidget.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode);
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    var values = this._getItemValues();
    var initial_value = this.getInitialValue();
    this.widget = this.domNode;
    attach_point.appendChild(this.domNode);
    for (var i = 0; i < values.length; i++)
    {
      if (!values[i].valid)
      {
        // always skip the invalid values for radios
        continue;
      }

      var radio_div = document.createElement("div");
      radio_div.style.lineHeight = "16px";
      this.widget.appendChild(radio_div);
      var radio = new Element("input");
      radio.setAttribute("id", this.id + "-widget");
      radio.setAttribute("name", this.id + "-widget");
      radio.setAttribute("type", "radio");
      radio_div.appendChild(radio);
      radio_div.appendChild(document.createTextNode(values[i].label));

      radio.setAttribute("value", values[i].value);
      if (values[i].value == initial_value)
      {
        this._selectedValue = initial_value;
        radio.checked = true;
      }
      if (this.isReadonly())
      {
        radio.setAttribute("disabled", true);
      }
      radio.onclick = this._radio_clickHandler.bindAsEventListener(this);
    }
    this.widget.style.height = this.widget.offsetHeight + "px";
  },

  /** */
  setValue: function(value, forceCommit)
  {
    if (!this.widget)
    {
      this.setInitialValue(value, forceCommit);
    }
    else
    {
      this.parent(value, forceCommit);
      this._selectedValue = value;
      var radios = this.widget.getElementsByTagName("input");
      for (var i = 0; i < radios.length; i++)
      {
        radios[i].checked = radios[i].getAttribute("value") == this._selectedValue;
      }
    }
  },

  getValue: function()
  {
    return this._selectedValue;
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _radio_clickHandler: function(event)
  { 
    var target;
    
    if (window.ie) 
    { 
      target = window.event.srcElement;
    }
    else 
    { 
      target = event.target;
    }
    
    if (!target.checked)
    {
      var all_radios = this.widget.getElementsByTagName("input");
      for (var i = 0; i < all_radios.length; i++)
      {
        if (all_radios[i].name == target.name)
        {
          all_radios[i].checked = target == all_radios[i];
        }
      }
    }
    
    this._selectedValue = target.value;
    this._commitValueChange();
  }
});

/** 
 * Handles xforms widget xf:select1.  Produces either a combobox or a set of
 * radios depending on the number of inputs.
 */
alfresco.xforms.ComboboxSelect1 = alfresco.xforms.AbstractSelectWidget.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode, new Element("select"));
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    var values = this._getItemValues();
    var initial_value = this.getInitialValue();
    this.domNode = new Element("select");
    attach_point.appendChild(this.domNode);
    this.widget = this.domNode;
    for (var i = 0; i < values.length; i++)
    {
      if (initial_value && !values[i].valid)
      {
        // skip the invalid value if we have a default value
        continue;
      }
      var option = new Element("option");
      this.widget.appendChild(option);
      option.appendChild(document.createTextNode(values[i].label));
      option.setAttribute("value", values[i].value);
      if (values[i].value == initial_value)
      {
        this._selectedValue = initial_value;
        option.selected = true;
      }

      if (this.isReadonly())
      {
        this.widget.setAttribute("disabled", true);
      }
    }
    this.widget.onchange = this._combobox_changeHandler.bindAsEventListener(this);
  },

  /** */
  setValue: function(value, forceCommit)
  {
    if (!this.widget)
    {
      this.setInitialValue(value, forceCommit);
    }
    else
    {
      this.parent(value, forceCommit);
      this._selectedValue = value;
      var options = this.widget.getElementsByTagName("option");
      for (var i = 0; i < options.length; i++)
      {
        options[i].selected = options[i].getAttribute("value") == this._selectedValue;
      }
    }
  },

  getValue: function()
  {
    return this._selectedValue;
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _combobox_changeHandler: function(event) 
  { 
    var target;
    
    if (window.ie) 
    { 
      target = window.event.srcElement;
    } 
    else 
    { 
      target = event.target;
    }

    this._selectedValue = target.options[target.selectedIndex].value;
    this._commitValueChange();
  }
});

/** 
 * Handles xforms widget xf:select1 with a type of boolean.
 */
alfresco.xforms.Checkbox = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, 
                xformsNode, 
                new Element("input", { type: "checkbox" }));
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    var initial_value = this.getInitialValue() == "true";
    attach_point.appendChild(this.domNode);
    this.widget = this.domNode;

    if (initial_value)
    {
      this.widget.setAttribute("checked", true);
    }
    if (this.isReadonly())
    {
      this.widget.setAttribute("disabled", true);
    }
    this.widget.onclick = this._checkbox_clickHandler.bindAsEventListener(this);
  },

  setValue: function(value, forceCommit)
  {
    if (!this.widget)
    {
      this.setInitialValue(value, forceCommit);
    }
    else
    {
      this.parent(value, forceCommit);
      this.widget.checked = value == "true";
    }
  },

  getValue: function()
  {
    return this.widget.checked;
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _checkbox_clickHandler: function(event)
  {
    this._commitValueChange();
  }
});

////////////////////////////////////////////////////////////////////////////////
// widgets for date types
////////////////////////////////////////////////////////////////////////////////

/** The date picker widget which handles xforms widget xf:input with type xf:date */
alfresco.xforms.DatePicker = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode);
    this._minInclusive = (_hasAttribute(this.xformsNode, alfresco.xforms.constants.ALFRESCO_PREFIX + ":minInclusive")
                          ? this.xformsNode.getAttribute(alfresco.xforms.constants.ALFRESCO_PREFIX + ":minInclusive")
                          : null);
    this._maxInclusive = (_hasAttribute(this.xformsNode, alfresco.xforms.constants.ALFRESCO_PREFIX + ":maxInclusive")
                          ? this.xformsNode.getAttribute(alfresco.xforms.constants.ALFRESCO_PREFIX + ":maxInclusive")
                          : null);

    dojo.require("dojo.date.format");
    // XXXarielb - change to a static
    this._noValueSet = (alfresco.resources["eg"] + " " + 
                        dojo.date.format(new Date(), 
                                         {datePattern: alfresco.xforms.constants.DATE_FORMAT, 
                                             selector: 'dateOnly'}));
  },

  _createPicker: function()
  {
    dojo.require("dojo.widget.DatePicker");
    var datePickerDiv = document.createElement("div");
    this.domNode.parentNode.appendChild(datePickerDiv);
      
    var dp_initial_value = this.getValue() || null; //dojo.date.toRfc3339(new Date());
    var datePickerProperties = { value: dp_initial_value };
    if (this._minInclusive)
    {
      datePickerProperties.startDate = this._minInclusive;
    }
    if (this._maxInclusive)
    {
      datePickerProperties.endDate = this._maxInclusive;
    }
    
    this.widget.picker = dojo.widget.createWidget("DatePicker", 
                                                  datePickerProperties,
                                                  datePickerDiv);
    this.domContainer.style.height = 
      Math.max(this.widget.picker.domNode.offsetHeight +
               this.widget.offsetHeight +
               this.domNode.parentNode.getStyle("margin-top").toInt() +
               this.domNode.parentNode.getStyle("margin-bottom").toInt(),
               20) + "px";

    dojo.event.connect(this.widget.picker,
                       "onValueChanged", 
                       this,
                       this._datePicker_valueChangedHandler);
  },

  _destroyPicker: function()
  {
    if (this.widget.picker)
    {
      this.domNode.parentNode.removeChild(this.widget.picker.domNode);
      this.widget.picker = null;
      this.domContainer.style.height = 
        Math.max(this.widget.offsetHeight +
                 this.domNode.parentNode.getStyle("margin-bottom").toInt() +
                 this.domNode.parentNode.getStyle("margin-top").toInt(),
                 20) + "px";
    }
  },
  
  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    var initial_value = this.getInitialValue();
    attach_point.appendChild(this.domNode);
    this.widget = new Element("input", { "id": this.id + "-widget", "type": "text"});
    if (initial_value)
    {
      var jsDate = dojo.date.fromRfc3339(initial_value);
      this.widget.setAttribute("value", 
                               dojo.date.format(jsDate,
                                                {datePattern: alfresco.xforms.constants.DATE_FORMAT, 
                                                 selector: 'dateOnly'}));
    }
    else
    {
      this.widget.setAttribute("value", this._noValueSet);
      this.widget.addClass("xformsGhostText");
    }
    if (this.isReadonly())
    {
      this.widget.setAttribute("disabled", true);
    }
    this.domNode.appendChild(this.widget);

    var expandoImage = new Element("img");
    expandoImage.setAttribute("src", alfresco.constants.WEBAPP_CONTEXT + "/images/icons/action.gif");
    expandoImage.align = "absmiddle";
    expandoImage.style.margin = "0px 5px";

    this.domNode.appendChild(expandoImage);

    if (!this.isReadonly())
    {
      expandoImage.onclick = this._expando_clickHandler.bindAsEventListener(this);
      this.widget.onfocus = this._dateTextBox_focusHandler.bindAsEventListener(this);
      this.widget.onchange = this._dateTextBox_changeHandler.bindAsEventListener(this);
    }
  },

  setValue: function(value, forceCommit)
  {
    if (!this.widget)
    {
      this.setInitialValue(value, forceCommit);
    }
    else
    {
      this.parent(value, forceCommit);
      var jsDate = dojo.date.fromRfc3339(value);
      this.widget.value = dojo.date.format(jsDate,
                                           {datePattern: alfresco.xforms.constants.DATE_FORMAT, 
                                            selector: 'dateOnly'});
      this.widget.removeClass("xformsGhostText");
    }
  },

  getValue: function()
  {
    if (this.widget.value == null || 
        this.widget.value.length == 0 ||
        this.widget.value == this._noValueSet)
    {
      return null;
    }
    else
    {
      var jsDate = dojo.date.parse(this.widget.value, 
                                   {datePattern: alfresco.xforms.constants.DATE_FORMAT, 
                                    selector: 'dateOnly'});
      return dojo.date.toRfc3339(jsDate, "dateOnly");
    }
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _dateTextBox_focusHandler: function(event)
  {
    this._destroyPicker();
  },

  _dateTextBox_changeHandler: function(event)
  {
    this._commitValueChange();
  },

  _datePicker_valueChangedHandler: function(date)
  {
    var rfcDate = dojo.date.toRfc3339(date, "dateOnly");
    this._destroyPicker();
    this.setValue(rfcDate);
    this._commitValueChange();
  },

  _expando_clickHandler: function()
  {
    if (this.widget.picker)
    {
      this._destroyPicker();
    }
    else
    {
      this._createPicker();
    }
  }
});

/** The date picker widget which handles xforms widget xf:input with type xf:date */
alfresco.xforms.TimePicker = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode);
    dojo.require("dojo.date.format");
    this._noValueSet = (alfresco.resources["eg"] + " " + 
                        dojo.date.format(new Date(), 
                                         {timePattern: alfresco.xforms.constants.TIME_FORMAT, 
                                             selector: "timeOnly"}));
    this._xformsFormat = "HH:mm:ss.S";
  },
  /** */
  _createPicker: function()
  {
    dojo.require("dojo.widget.TimePicker");
    var timePickerDiv = document.createElement("div");
    this.domNode.appendChild(timePickerDiv);
    var jsDate = (this.getValue()
                  ? dojo.date.parse(this.getValue(),
                                    {timePattern: this._xformsFormat, 
                                     selector: "timeOnly"})
                  : new Date());
    this.widget.picker = dojo.widget.createWidget("TimePicker", 
                                                  { 
                                                    value: jsDate
                                                  }, 
                                                  timePickerDiv);
    this.widget.picker.anyTimeContainerNode.innerHTML = "";

    // don't let it float - it screws up layout somehow
    this.widget.picker.domNode.style.cssFloat = "none";
    this.domContainer.style.height = 
      Math.max(this.widget.picker.domNode.offsetHeight +
               this.widget.offsetHeight +
               this.domNode.parentNode.getStyle("margin-top").toInt() +
               this.domNode.parentNode.getStyle("margin-bottom").toInt(),
               20) + "px";
    dojo.event.connect(this.widget.picker,
                       "onValueChanged", 
                       this,
                       this._timePicker_valueChangedHandler);
  },

  _destroyPicker: function()
  {
    if (this.widget.picker)
    {
      this.domNode.removeChild(this.widget.picker.domNode);
      this.widget.picker = null;
      this.domContainer.style.height = 
        Math.max(this.widget.offsetHeight +
                 this.domNode.parentNode.getStyle("margin-top").toInt() +
                 this.domNode.parentNode.getStyle("margin-bottom").toInt(),
                 20) + "px";
    }
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    var initial_value = this.getInitialValue();
  
    attach_point.appendChild(this.domNode);
    this.widget = new Element("input", { "id": this.id + "-widget", "type": "text" });
    if (initial_value)
    {
      var jsDate = dojo.date.parse(initial_value, {timePattern: this._xformsFormat, selector: "timeOnly"});
      this.widget.setAttribute("value",
                               dojo.date.format(jsDate,
                                                {timePattern: alfresco.xforms.constants.TIME_FORMAT,
                                                 selector: "timeOnly"}));
    }
    else
    {
      this.widget.setAttribute("value", this._noValueSet);
      this.widget.addClass("xformsGhostText");
    }
    if (this.isReadonly())
    {
      this.widget.setAttribute("disabled", true);
    }
    this.domNode.appendChild(this.widget);

    var expandoImage = new Element("img");
    expandoImage.setAttribute("src", alfresco.constants.WEBAPP_CONTEXT + "/images/icons/action.gif");
    expandoImage.align = "absmiddle";
    expandoImage.style.margin = "0px 5px";

    this.domNode.appendChild(expandoImage);

    if (!this.isReadonly())
    {
      expandoImage.onclick = this._expando_clickHandler.bindAsEventListener(this);
      this.widget.onfocus = this._timeTextBox_focusHandler.bindAsEventListener(this);
      this.widget.onchange = this._timeTextBox_changeHandler.bindAsEventListener(this);
    }
  },

  setValue: function(value, forceCommit)
  {
    if (!this.widget)
    {
      this.setInitialValue(value, forceCommit);
    }
    else
    {
      this.parent(value, forceCommit);
      var jsDate = dojo.date.parse(value, {timePattern: this._xformsFormat, selector: "timeOnly"});
      this.widget.value = dojo.date.format(jsDate,
                                           {timePattern: alfresco.xforms.constants.TIME_FORMAT,
                                            selector: "timeOnly"});
      this.widget.removeClass("xformsghosttext");
    }
  },

  getValue: function()
  {
    if (this.widget.value == null ||
        this.widget.value.length == 0 ||
        this.widget.value == this._noValueSet)
    {
      return null;
    }
    else
    {
      var jsDate = dojo.date.parse(this.widget.value,
                                   {timePattern: alfresco.xforms.constants.TIME_FORMAT,
                                    selector: "timeOnly"});
      return dojo.date.format(jsDate, {timePattern: this._xformsFormat, selector: "timeOnly"});
    }
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////
  
  _timeTextBox_focusHandler: function(event)
  {
    this._destroyPicker();
  },

  _timeTextBox_changeHandler: function(event)
  {
    this._commitValueChange();
  },

  _timePicker_valueChangedHandler: function(date)
  {
    var xfDate = dojo.date.format(date, {timePattern: this._xformsFormat, selector: "timeOnly"});
    this.setValue(xfDate);
    this._commitValueChange();
  },

  _expando_clickHandler: function()
  {
    if (this.widget.picker)
    {
      this._destroyPicker();
    }
    else
    {
      this._createPicker();
    }
  }
});

/** The date time picker widget which handles xforms widget xf:input with type xf:datetime */
alfresco.xforms.DateTimePicker = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode) 
  { 
    this.parent(xform, xformsNode);
    dojo.require("dojo.date.format");
    this._noValueSet = (alfresco.resources["eg"] + " " + 
                        dojo.date.format(new Date(), 
                                         {datePattern: alfresco.xforms.constants.DATE_TIME_FORMAT,
                                          selector: "dateOnly"}));
  },

  /** */
  _createPicker: function()
  {
    dojo.require("dojo.widget.DatePicker");
    dojo.require("dojo.widget.TimePicker");
    
    this._pickerDiv = document.createElement("div");
    this._pickerDiv.style.position = "relative";
    this._pickerDiv.style.width = this.widget.offsetWidth + "px";
    this.domNode.appendChild(this._pickerDiv);

    var datePickerDiv = document.createElement("div");
    datePickerDiv.style.position = "absolute";
    datePickerDiv.style.left = "0px";
    datePickerDiv.style.top = "0px";
    this._pickerDiv.appendChild(datePickerDiv);

    var dp_initial_value = this.getValue() || dojo.date.toRfc3339(new Date());
    this.widget.datePicker = dojo.widget.createWidget("DatePicker",
                                                      {
                                                        value: dp_initial_value
                                                      },
                                                      datePickerDiv);
    var timePickerDiv = document.createElement("div");
    timePickerDiv.style.position = "absolute";
    timePickerDiv.style.right = "0px";
    timePickerDiv.style.top = "0px";
    this._pickerDiv.appendChild(timePickerDiv);

    var jsDate = this.getValue() ? dojo.date.fromRfc3339(this.getValue()) : new Date();
    this.widget.timePicker = dojo.widget.createWidget("TimePicker", 
                                                      { 
                                                        value: jsDate
                                                      }, 
                                                      timePickerDiv);
    this.widget.timePicker.anyTimeContainerNode.innerHTML = "";

    // don't let it float - it screws up layout somehow
    this.widget.timePicker.domNode.style.cssFloat = "none";
    this._pickerDiv.style.height = Math.max(this.widget.timePicker.domNode.offsetHeight,
                                            this.widget.datePicker.domNode.offsetHeight);
    this.domContainer.style.height = 
      Math.max(this._pickerDiv.offsetHeight +
               this.widget.offsetHeight +
               this.domNode.parentNode.getStyle("margin-top").toInt() +
               this.domNode.parentNode.getStyle("margin-bottom").toInt(),
               20) + "px";
    dojo.event.connect(this.widget.datePicker,
                       "onValueChanged", 
                       this,
                       this._datePicker_valueChangedHandler);
    dojo.event.connect(this.widget.timePicker,
                       "onValueChanged", 
                       this,
                       this._timePicker_valueChangedHandler);
  },

  _destroyPicker: function()
  {
    if (this._pickerDiv)
    {
      this.domNode.removeChild(this._pickerDiv);
      this.widget.datePicker = null;
      this.widget.timePicker = null;
      this._pickerDiv = null;
      this.domContainer.style.height = 
        Math.max(this.widget.offsetHeight +
                 this.domNode.parentNode.getStyle("margin-top").toInt() +
                 this.domNode.parentNode.getStyle("margin-bottom").toInt(),
                 20) + "px";
    }
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    var initial_value = this.getInitialValue();
  
    attach_point.appendChild(this.domNode);
    this.widget = new Element("input", { "id": this.id + "-widget", "type": "text" });
    if (initial_value)
    {
      var jsDate = dojo.date.fromRfc3339(initial_value);
      this.widget.setAttribute("value",
                               dojo.date.format(jsDate,
                                                {timePattern: alfresco.xforms.constants.DATE_TIME_FORMAT,
                                                 selector: "timeOnly"}));
    }
    else
    {
      this.widget.setAttribute("value", this._noValueSet);
      this.widget.addClass("xformsGhostText");
    }
    this.domNode.appendChild(this.widget);
    this.widget.style.width = (3 * this.widget.offsetWidth) + "px";

    var expandoImage = new Element("img");
    expandoImage.setAttribute("src", alfresco.constants.WEBAPP_CONTEXT + "/images/icons/action.gif");
    expandoImage.align = "absmiddle";
    expandoImage.style.margin = "0px 5px";

    this.domNode.appendChild(expandoImage);

    expandoImage.onclick = this._expando_clickHandler.bindAsEventListener(this);
    this.widget.onfocus = this._dateTimeTextBox_focusHandler.bindAsEventListener(this);
    this.widget.onchange = this._dateTimeTextBox_changeHandler.bindAsEventListener(this);
  },

  setValue: function(value, forceCommit)
  {
    if (!this.widget)
    {
      this.setInitialValue(value, forceCommit);
    }
    else
    {
      this.parent(value, forceCommit);
      var jsDate = dojo.date.fromRfc3339(value);
      this.widget.value = dojo.date.format(jsDate,
                                           {datePattern: alfresco.xforms.constants.DATE_TIME_FORMAT,
                                            selector: "dateOnly"});
      this.widget.removeClass("xformsGhostText");
    }
  },

  getValue: function()
  {
    if (this.widget.value == null ||
        this.widget.value.length == 0 ||
        this.widget.value == this._noValueSet)
    {
      return null;
    }
    else
    {
      var jsDate = dojo.date.parse(this.widget.value,
                                   {datePattern: alfresco.xforms.constants.DATE_TIME_FORMAT,
                                    selector: "dateOnly"});
      return dojo.date.toRfc3339(jsDate);
    }
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////
  
  _dateTimeTextBox_focusHandler: function(event)
  {
    this._destroyPicker();
  },

  _dateTimeTextBox_changeHandler: function(event)
  {
    this._commitValueChange();
  },

  _timePicker_valueChangedHandler: function(date)
  {
    var value = this.getValue() ? dojo.date.fromRfc3339(this.getValue()) : new Date();
    value.setHours(date.getHours());
    value.setMinutes(date.getMinutes());
    value = dojo.date.toRfc3339(value);
    this.setValue(value);
    this._commitValueChange();
  },

  _datePicker_valueChangedHandler: function(date)
  {
    var value = this.getValue() ? dojo.date.fromRfc3339(this.getValue()) : new Date();
    value.setYear(date.getYear());
    value.setMonth(date.getMonth());
    value.setDate(date.getDate());
    value = dojo.date.toRfc3339(value);
    this.setValue(value);
    this._commitValueChange();
  },

  _expando_clickHandler: function()
  {
    if (this._pickerDiv)
    {
      this._destroyPicker();
    }
    else
    {
      this._createPicker();
    }
  }
});

/** The year picker handles xforms widget xf:input with a gYear type */
alfresco.xforms.YearPicker = alfresco.xforms.TextField.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode);
  },


  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  render: function(attach_point)
  {
    this.parent(attach_point);
    this.widget.size = "4";
    this.widget.setAttribute("maxlength", "4");
  },

  getInitialValue: function()
  {
    var result = this.parent();
    return result ? result.replace(/^0*([^0]+)$/, "$1") : result;
  },

  setValue: function(value, forceCommit)
  {
    this.parent((value 
                 ? value.replace(/^0*([^0]+)$/, "$1") 
                 : null), 
                forceCommit);
  },

  getValue: function()
  {
    var result = this.parent();
    return result ? dojo.string.padLeft(result, 4, "0") : null;
  }
});

/** The day picker widget which handles xforms widget xf:input with type xf:gDay */
alfresco.xforms.DayPicker = alfresco.xforms.ComboboxSelect1.extend({
  initialize: function(xform, xformsNode)
  {
    this.parent(xform, xformsNode);
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////
  _getItemValues: function()
  {
    var result = [];
    result.push({id: "day_empty", label: "", value: "", valid: false});
    for (var i = 1; i <= 31; i++)
    {
      result.push({
            id: "day_" + i, 
            label: i, 
            value: "---" + (i < 10 ? "0" + i : i),
            valid: true
      });
    }
    return result;
  }
});

/** The month picker widget which handles xforms widget xf:input with type xf:gMonth */
alfresco.xforms.MonthPicker = alfresco.xforms.ComboboxSelect1.extend({
  initialize: function(xform, xformsNode)
  {
    this.parent(xform, xformsNode);
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////
  _getItemValues: function()
  {
    var result = [];
    result.push({id: "month_empty", label: "", value: "", valid: false});
    for (var i = 0; i < 12; i++)
    {
      var d = new Date();
      d.setMonth(i);
      result.push({
            id: "month_" + i, 
            label: dojo.date.getMonthName(d),
            value: "--" + (i + 1 < 10 ? "0" + (i + 1) : i + 1),
            valid: true
      });
    }
    return result;
  }
});

/** The month day picker widget which handles xforms widget xf:input with type xf:gMonthDay */
alfresco.xforms.MonthDayPicker = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode)
  {
    this.parent(xform, xformsNode);
    this.monthPicker = new alfresco.xforms.MonthPicker(xform, xformsNode);
    this.monthPicker._compositeParent = this;
               
    this.dayPicker = new alfresco.xforms.DayPicker(xform, xformsNode);
    this.dayPicker._compositeParent = this;
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////
  render: function(attach_point)
  {
    this.setValue(this.getInitialValue());
    attach_point.appendChild(this.domNode);
    this.dayPicker.render(this.domNode); 
    this.dayPicker.widget.style.marginRight = "10px";
    this.monthPicker.render(this.domNode);
    this.domNode.style.width = this.monthPicker.domNode.offsetWidth + this.dayPicker.domNode.offsetWidth + 10 + "px";
  },
    
  setValue: function(value)
  {
    this.monthPicker.setValue(value ? value.match(/^--[^-]+/)[0] : null);
    this.dayPicker.setValue(value ? "---" + value.replace(/^--[^-]+-/, "") : null);
  },
    
  getValue: function()
  {
    // format is --MM-DD
    var day = this.dayPicker.getValue();
    var month = this.monthPicker.getValue();
    return month && day ? day.replace(/^--/, month) : null;
  }
});

/** The year month picker widget which handles xforms widget xf:input with type xf:gYearMonth */
alfresco.xforms.YearMonthPicker = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode)
  {
    this.parent(xform, xformsNode);
    this.yearPicker = new alfresco.xforms.YearPicker(xform, xformsNode);
    this.yearPicker._compositeParent = this;
               
    this.monthPicker = new alfresco.xforms.MonthPicker(xform, xformsNode);
    this.monthPicker._compositeParent = this;
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////
  render: function(attach_point)
  {
    this.setValue(this.getInitialValue());
    attach_point.appendChild(this.domNode);
    this.monthPicker.render(this.domNode);
    this.monthPicker.widget.style.marginRight = "10px";
    this.yearPicker.domNode.style.display = "inline";
    this.yearPicker.render(this.domNode);
    this.domNode.style.width = this.yearPicker.domNode.offsetWidth + this.monthPicker.domNode.offsetWidth + 10 + "px";
  },

  setValue: function(value)
  {
    this.monthPicker.setValue(value ? value.replace(/^[^-]+-/, "--") : null);
    this.yearPicker.setValue(value ? value.match(/^[^-]+/)[0] : null);
  },

  getValue: function()
  {
    // format is CCYY-MM
    var year = this.yearPicker.getValue();
    var month = this.monthPicker.getValue();
    return year && month ? month.replace(/^-/, year) : null;
  }
});

////////////////////////////////////////////////////////////////////////////////
// widgets for group types
////////////////////////////////////////////////////////////////////////////////

/** 
 * Handles xforms widget xf:group.  A group renders and manages a set of children
 * and provides a header for expanding and collapsing the group.  A group header
 * is shown for all group that don't have xf:appearance set to 'repeated' and 
 * that are not the root group.
 */
alfresco.xforms.AbstractGroup = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode, domNode) 
  {
    this.parent(xform, xformsNode, domNode);
    this._children = [];
    this.domNode.removeClass("xformsItem");
  },

  /////////////////////////////////////////////////////////////////
  // methods & properties
  /////////////////////////////////////////////////////////////////

  /** Returns the child at the specified index or null if the index is out of range. */
  getChildAt: function(index)
  {     
    return index < this._children.length ? this._children[index] : null;
  },

  /** Returns the index of a particular child or -1 if the child was not found. */
  getChildIndex: function(child)
  {
    for (var i = 0; i < this._children.length; i++)
    {
      if (alfresco.constants.DEBUG)
      {
        alfresco.log(this.id + "[" + i + "]: " + 
                   " is " + this._children[i].id + 
                   " the same as " + child.id + "?");
      }
      if (this._children[i] == child)
      {
        return i;
      }
    }
    return -1;
  },

  /** Adds the child to end of the list of children. */
  addChild: function(child)
  {
    return this._insertChildAt(child, this._children.length);
  },

  _isIndented: function()
  {
    return !(this.parentWidget instanceof alfresco.xforms.ViewRoot) && this._children.length > 1;
  },

  /** Inserts a child at the specified position. */
  _insertChildAt: function(child, position, nodeName, attach_point)
  {
    alfresco.log(this.id + "._insertChildAt(" + child.id + ", " + position + ")");
    child.parentWidget = this;

    child.domContainer = new Element($pick(nodeName, "div"));
    child.domContainer.setAttribute("id", child.id + "-domContainer");
    child.domContainer.addClass("xformsItemDOMContainer");

    if (position == this._children.length)
    {
      $pick(attach_point, this.domNode.childContainerNode).appendChild(child.domContainer);
      this._children.push(child);
    }
    else
    {
      $pick(attach_point, this.domNode.childContainerNode).insertBefore(child.domContainer, 
                                                                        this.getChildAt(position).domContainer);
      this._children.splice(position, 0, child);
    }
    return child.domContainer;
  },

  /** Removes the child at the specified position. */
  _removeChildAt: function(position)
  {
    var child = this.getChildAt(position);
    if (!child)
    {
      throw new Error("unable to find child at " + position);
    }

    this._children.splice(position, 1);
    child.domContainer.group = this;
    var anim = dojo.lfx.html.fadeOut(child.domContainer, 500);
    anim.onEnd = function()
      {
        child.domContainer.style.display = "none";
        child._destroy();

        child.domContainer.remove();

        child.domContainer.group._updateDisplay(false);
      };
    anim.play();

    this._childRemoved(child);

    return child;
  },

  /** Event handler for when a child has been added. */
  _childAdded: function(child) { },

  /** Event handler for when a child has been removed. */
  _childRemoved: function(child) { },

  /** Utility function to create the a label container */
  _createLabelContainer: function(child, nodeName, attach_point)
  {
    var labelNode = new Element($pick(nodeName, "div"), 
                                { 
                                  id: child.id + "-label", 
                                  "class": "xformsItemLabelContainer"
                                });
    var requiredImage = new Element("img", { "class": "xformsItemRequiredImage" });
    requiredImage.src = alfresco.xforms.AbstractGroup._requiredImage.src;

    labelNode.appendChild(requiredImage);

    if (!child.isRequired())
    {
      requiredImage.style.visibility = "hidden";
    }
    var label = child.getLabel();
    if (label)
    {
      child.labelNode = labelNode;
      child.labelNode.appendChild(document.createTextNode(label));
    }
    var hint = child.getHint();
    if (hint)
    {
      labelNode.setAttribute("title", hint);
      requiredImage.setAttribute("alt", hint);
    }
    labelNode.style.width = "0px";
    $pick(attach_point, child.domContainer).appendChild(labelNode);
    labelNode.style.width = labelNode.scrollWidth + "px";
    return labelNode;
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods & properties
  /////////////////////////////////////////////////////////////////

  isValidForSubmit: function()
  {
    return true;
  },

  /** Iterates all children a produces an array of widgets which are invalid for submit. */
  getWidgetsInvalidForSubmit: function()
  {
    var result = [];
    for (var i = 0; i < this._children.length; i++)
    {
      if (this._children[i] instanceof alfresco.xforms.AbstractGroup)
      {
        result = result.concat(this._children[i].getWidgetsInvalidForSubmit());
      }
      else if (!this._children[i].isValidForSubmit())
      {
        result.push(this._children[i]);
      }
    }
    return result;
  },

  /** Recusively destroys all children. */
  _destroy: function()
  {
    this.parent();
    this._children.each(function(c) { c._destroy() });
  },

  setReadonly: function(readonly)
  {
    this.parent(readonly);
    this._children.each(function(c) { c.setReadonly(readonly); });
  },

  render: function(attach_point)
  {
    this.domNode.widget = this;
    return this.domNode;
  },

  _updateDisplay: function(recursively)
  {
    if (recursively)
    {
      this._children.each(function(c) { c._updateDisplay(recursively); });
    }
  },

  showAlert: function()
  {
    this._children.each(function(c) { c.showAlert(); });
  },

  hideAlert: function()
  {
    this._children.each(function(c) { c.hideAlert(); });
  }
});

alfresco.xforms.AbstractGroup._requiredImage = new Image();
alfresco.xforms.AbstractGroup._requiredImage.src = alfresco.constants.WEBAPP_CONTEXT + "/images/icons/required_field.gif";

/** 
 * Handles xforms widget xf:group.  A group renders and manages a set of children
 * and provides a header for expanding and collapsing the group.  A group header
 * is shown for all group that don't have xf:appearance set to 'repeated' and 
 * that are not the root group.
 */
alfresco.xforms.VGroup = alfresco.xforms.AbstractGroup.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode);
  },

  /////////////////////////////////////////////////////////////////
  // methods & properties
  /////////////////////////////////////////////////////////////////

  _groupHeaderNode: null,

  /////////////////////////////////////////////////////////////////
  // overridden methods & properties
  /////////////////////////////////////////////////////////////////

  /** Inserts a child at the specified position. */
  _insertChildAt: function(child, position)
  {
    if (!this.domNode.childContainerNode.parentNode)
    {
      // only add this to the dom once we're adding a child
      this.domNode.appendChild(this.domNode.childContainerNode);
      this._contentDivs = {};
    }

    child.domContainer = this.parent(child, position);

    if (this.parentWidget && this.parentWidget.domNode)
    {
      child.domContainer.style.top = this.parentWidget.domNode.style.bottom;
    }

    function shouldInsertDivider(group, child, position)
    {
      if (group.getAppearance() != "full")
      {
        return false;
      }
      if (group instanceof alfresco.xforms.Repeat)
      {
        return false;
      }

      if (!child.isVisible())
      {
        return false;
      }
      if (group._children[position - 1] instanceof alfresco.xforms.AbstractGroup)
      {
        return true;
      }
      if (child instanceof alfresco.xforms.AbstractGroup)
      {
        for (var i = position - 1; i > 0; i--)
        {
          if (group._children[i].isVisible())
          {
            return true;
          }
        }
      }
      return false;
    }

    if (shouldInsertDivider(this, child, position))
    {
      var divider = new Element("div", { "class": "xformsGroupDivider"});
      this.domNode.childContainerNode.insertBefore(divider,
                                                   child.domContainer);
    }

    var contentDiv = new Element("div", { "id":  child.id + "-content", "class": "xformsGroupItem"});
    this._contentDivs[child.id] = contentDiv;
    if (!(child instanceof alfresco.xforms.AbstractGroup))
    {
      contentDiv.labelNode = this._createLabelContainer(child);
      child.domContainer.appendChild(contentDiv.labelNode);
    }

    child.domContainer.appendChild(contentDiv);
    contentDiv.style.left = (child instanceof alfresco.xforms.AbstractGroup 
                             ? "0px" 
                             : "30%");

    contentDiv.style.width = (child instanceof alfresco.xforms.AbstractGroup
                              ? "100%"
                              : (1 - (contentDiv.offsetLeft / 
                                      child.domContainer.offsetWidth)) * 100 + "%");
    child.render(contentDiv);
    if (!(child instanceof alfresco.xforms.AbstractGroup))
    {
      child.domContainer.style.height = 
        Math.max(contentDiv.offsetHeight +
                 contentDiv.getStyle("margin-top").toInt() +
                 contentDiv.getStyle("margin-bottom").toInt(),
                 20) + "px";
    }

    alfresco.log(contentDiv.getAttribute("id") + " offsetTop is " + contentDiv.offsetTop);
    contentDiv.style.top = "-" + Math.max(0, contentDiv.offsetTop - contentDiv.getStyle("margin-top").toInt()) + "px";
    if (contentDiv.labelNode)
    {
//      contentDiv.labelNode.style.top = (contentDiv.offsetTop + ((.5 * contentDiv.offsetHeight) -
//                                                                (.5 * contentDiv.labelNode.offsetHeight))) + "px";
      contentDiv.labelNode.style.position = "relative";
      contentDiv.labelNode.style.top = contentDiv.offsetTop + "px";
      contentDiv.labelNode.style.height = contentDiv.offsetHeight + "px";
      contentDiv.labelNode.style.lineHeight = contentDiv.labelNode.style.height;

    }
    contentDiv.widget = child;

    // Glen.Johnson@alfresco.com - for each child added to a VGroup,
    // the method call below (commented out) recalculates the layout and 
    // updates the display for each of its siblings (already displayed 
    // above it). This is extremely expensive in terms of processing time.
    // Commenting out the method call below drastically improves form rendering
    // time for forms containing lots of VGroup widgets.
    // See JIRA issue WCM-629
    //
    // this._updateDisplay(false);
    
    this._childAdded(child);
    return child.domContainer;
  },

  render: function(attach_point)
  {
    this.domNode.widget = this;

    if (this.getAppearance() == "full")
    {
      this.domNode.addClass("xformsGroup");
      this.domNode.style.position = "relative";
      this.domNode.style.marginRight = (this.domNode.getStyle("margin-left").toInt() / 3) + "px";
      if (window.ie)
      {
        this.domNode.style.width = "100%";
      }
      else
      {
        var x = ((this.domNode.offsetWidth - this.domNode.clientWidth) + 
                 this.domNode.getStyle("margin-left").toFloat() + 
                 this.domNode.getStyle("margin-right").toFloat());
        this.domNode.style.width = (1 - (x / attach_point.offsetWidth)) * 100 + "%";
      }

      this._groupHeaderNode = new Element("div", 
                                          {
                                            "id": this.id + "-groupHeaderNode",
                                            "class": "xformsGroupHeader"
                                          });
      this.domNode.appendChild(this._groupHeaderNode);

      this.toggleExpandedImage = new Element("img",
                                             {
                                               "align": "absmiddle",
                                               "styles": { "margin": "0px 5px" },
                                               "src": alfresco.xforms.constants.EXPANDED_IMAGE.src
                                             });
      this._groupHeaderNode.appendChild(this.toggleExpandedImage);
      this.toggleExpandedImage.onclick = this._toggleExpanded_clickHandler.bindAsEventListener(this);
      
      this._groupHeaderNode.appendChild(document.createTextNode(this.getLabel()));
    }
    attach_point.appendChild(this.domNode);
    this.domNode.childContainerNode = new Element("div",
                                                  {
                                                    "id": this.id + "-childContainerNode",
                                                    "styles": { "position": "relative", "width": "100%" }
                                                  });
    return this.domNode;
  },

  /** Indicates if the group is expanded. */
  isExpanded: function()
  {
    return (this.toggleExpandedImage.getAttribute("src") == 
            alfresco.xforms.constants.EXPANDED_IMAGE.src);
  },

  /** 
   * Sets the expanded state of the widget.  If collapsed, everything but the header 
   * will be hidden.
   */
  setExpanded: function(expanded)
  {
    if (expanded != this.isExpanded())
    {
      this.toggleExpandedImage.src = 
        (expanded 
         ? alfresco.xforms.constants.EXPANDED_IMAGE.src 
         : alfresco.xforms.constants.COLLAPSED_IMAGE.src);
      this.domNode.childContainerNode.style.display = expanded ? "block" : "none";
    }
  },

  _updateDisplay: function(recursively)
  {
    if (this._isIndented())
    {
      this.domNode.style.marginLeft = 10 + "px";
      this.domNode.style.marginRight = 5 + "px";
      // XXXarielb can this be moved to render or insertChild?
      this.domNode.style.width = (((this.domNode.offsetWidth - 15) / this.domNode.offsetWidth) * 100) + "%";
    }
    if (window.ie)
    {
      this.domNode.style.width = "100%";
    }
    else
    {
//      var x = ((this.domNode.offsetWidth - this.domNode.clientWidth) + 
//               this.domNode.getStyle("margin-left").toFloat() + 
//               this.domNode.getStyle("margin-right").toFloat());
//      this.domNode.style.width = (1 - (x / this.domNode.parentNode.offsetWidth)) * 100 + "%";
    }

    for (var i = 0; i < this._children.length; i++)
    {
      if (!this._children[i].isVisible())
      {
        continue;
      }
      var contentDiv = this._contentDivs[this._children[i].id];

      contentDiv.style.position = "static";
      contentDiv.style.top = "0px";
      contentDiv.style.left = "0px";

      contentDiv.style.position = "relative";
      contentDiv.style.left = (this._children[i] instanceof alfresco.xforms.AbstractGroup
                               ? "0px"
                               : "30%");
      contentDiv.style.width = (this._children[i] instanceof alfresco.xforms.AbstractGroup
                                ? "100%"
                                : (1 - (contentDiv.offsetLeft / 
                                        this._children[i].domContainer.parentNode.offsetWidth)) * 100 + "%");

      if (recursively)
      {
        this._children[i]._updateDisplay(recursively);
      }

      if (!(this._children[i] instanceof alfresco.xforms.AbstractGroup))
      {
        this._children[i].domContainer.style.height =
          Math.max(contentDiv.offsetHeight +
                   contentDiv.getStyle("margin-top").toInt() +
                   contentDiv.getStyle("margin-bottom").toInt(),
                   20) + "px";
      }

      contentDiv.style.top = "-" + Math.max(0, contentDiv.offsetTop - contentDiv.getStyle("margin-top").toInt()) + "px";

      var labelNode = contentDiv.labelNode;
      if (labelNode)
      {
//        labelNode.style.position = "static";
//        labelNode.style.top = "0px";
//        labelNode.style.left = "0px";
//        labelNode.style.position = "relative";

//        labelNode.style.top = (contentDiv.offsetTop + ((.5 * contentDiv.offsetHeight) -
//                                                      (.5 * labelNode.offsetHeight))) + "px";
      }
    }
  },
  
  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _toggleExpanded_clickHandler: function(event)
  {
    this.setExpanded(!this.isExpanded());
  }
});

/** 
 * Handles xforms widget xf:group.  A group renders and manages a set of children
 * and provides a header for expanding and collapsing the group.  A group header
 * is shown for all group that don't have xf:appearance set to 'repeated' and 
 * that are not the root group.
 */
alfresco.xforms.HGroup = alfresco.xforms.AbstractGroup.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode, new Element("table"));
  },

  /////////////////////////////////////////////////////////////////
  // methods & properties
  /////////////////////////////////////////////////////////////////

  /** a map of child ids to contentDivs */
  _contentDivs: {},

  /////////////////////////////////////////////////////////////////
  // overridden methods & properties
  /////////////////////////////////////////////////////////////////

  _isIndented: function()
  {
    return false;
  },

  /** Inserts a child at the specified position. */
  _insertChildAt: function(child, position)
  {
    var labelCell = new Element("td");
    this.domNode.childContainerNode.appendChild(labelCell);
    var labelNode = this._createLabelContainer(child, "div", labelCell);
    labelNode.style.minWidth = "40px";
    labelCell.style.width = labelNode.style.width;

    child.parentWidget = this;
    
    var contentCell = new Element("td");
    this.domNode.childContainerNode.appendChild(contentCell);
    child.domContainer = this.parent(child, position, null, contentCell);
    child.domContainer.style.position = "static";

    var contentDiv = new Element("div", { id: child.id + "-content", "class": "xformsGroupItem" });
    child.domContainer.appendChild(contentDiv);
    this._contentDivs[child.id] = contentDiv;

    contentDiv.labelNode = labelNode;
    child.render(contentDiv);
    
    var w = child.domNode.style.width;
    if (!w || w[w.length - 1] != "%")
    {
      contentCell.style.width = child.domNode.offsetWidth + "px";
    }
    contentDiv.widget = child;
    this._childAdded(child);
    return child.domContainer;
  },

  render: function(attach_point)
  {
    this.domNode.widget = this;
    this.domNode.style.width = "100%";
    attach_point.appendChild(this.domNode);

    var tbody = new Element("tbody");
    this.domNode.appendChild(tbody);
    this.domNode.childContainerNode = new Element("tr");
    tbody.appendChild(this.domNode.childContainerNode);
    return this.domNode;
  },

  _updateDisplay: function(recursively)
  {
    this._children.each(function(child, index)
                        {
                          if (recursively)
                          {
                            child._updateDisplay(recursively);
                          }
                        }.bind(this));
  }
});

alfresco.xforms.SwitchGroup = alfresco.xforms.VGroup.extend({
  initialize: function(xform, xformsNode)
  {
    this.parent(xform, xformsNode);
    if (this.getInitialValue())
    {
      var initialValueTrigger = this._getCaseToggleTriggerByTypeValue(this.getInitialValue());
      this._selectedCaseId = initialValueTrigger.getActions()["toggle"].properties["case"];
    }
  },

  /////////////////////////////////////////////////////////////////
  // methods & properties
  /////////////////////////////////////////////////////////////////
  _getCaseToggleTriggers: function()
  {
    var bw = this.xform.getBinding(this.xformsNode).widgets;
    var result = [];
    for (var i in bw)
    {
      if (! (bw[i] instanceof alfresco.xforms.Trigger))
      {
        continue;
      }
      
      var action = bw[i].getActions()["toggle"];
      if (action)
      {
        result.push(bw[i]);
      }
    }
    return result;
  },

  _getCaseToggleTriggerByCaseId: function(caseId)
  {
    var bw = this.xform.getBinding(this.xformsNode).widgets;
    for (var i in bw)
    {
      if (! (bw[i] instanceof alfresco.xforms.Trigger))
      {
        continue;
      }
      
      var action = bw[i].getActions()["toggle"];
      if (!action)
      {
        continue;
      }
      if (action.properties["case"] == caseId)
      {
        return bw[i];
      }
    }
    throw new Error("unable to find trigger " + type + 
                    ", properties " + properties +
                    " for " + this.id);

  },

  _getCaseToggleTriggerByTypeValue: function(typeValue)
  {
    var bw = this.xform.getBinding(this.xformsNode).widgets;
    for (var i in bw)
    {
      if (! (bw[i] instanceof alfresco.xforms.Trigger))
      {
        continue;
      }
      
      var action = bw[i].getActions()["setvalue"];
      if (!action)
      {
        continue;
      }
      if (action.properties["value"] == typeValue)
      {
        return bw[i];
      }
    }
    throw new Error("unable to find toggle trigger for type value " + typeValue + 
                    " for " + this.id);
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods & properties
  /////////////////////////////////////////////////////////////////

  /** */
  _insertChildAt: function(child, position)
  {
    var childDomContainer = this.parent(child, position);
    if (child.id == this._selectedCaseId)
    {
      this._getCaseToggleTriggerByCaseId(this._selectedCaseId).fire();
    }
    else
    {
      childDomContainer.style.display = "none";
    }
    return childDomContainer;
  },

  render: function(attach_point)
  {
    this.parent(attach_point);
    var cases = this._getCaseToggleTriggers();
    var caseToggleSelect = new Element("select",
                                       {
                                         "id": this.id + "-toggle-select",
                                         "styles": { "position": "absolute", "right": "0px", "top": "0px" }
                                       });
    this._groupHeaderNode.appendChild(caseToggleSelect);
    for (var i = 0; i < cases.length; i++)
    {
      var option = document.createElement("option");
      caseToggleSelect.appendChild(option);
      var caseId = cases[i].getActions()["toggle"].properties["case"];
      option.setAttribute("value", caseId);
      option.appendChild(document.createTextNode(cases[i].getLabel()));
      if (cases[i].getActions()["toggle"].properties["case"] == this._selectedCaseId)
      {
        option.selected = true;
      }
    }
    caseToggleSelect.onchange = this._caseToggleSelect_changeHandler.bindAsEventListener(this);
  },

  /////////////////////////////////////////////////////////////////
  // XForms event handlers
  /////////////////////////////////////////////////////////////////

  /** */
  handleSwitchToggled: function(selectedCaseId, deselectedCaseId)
  {
    alfresco.log(this.id + ".handleSwitchToggled(" + selectedCaseId + 
               ", " + deselectedCaseId + ")");
    this.selectedCaseId = selectedCaseId;
    for (var i = 0; i < this._children.length; i++)
    {
      if (this._children[i].id == selectedCaseId)
      {
        this._children[i].domContainer.style.display = "block";
      }
      else if (this._children[i].id == deselectedCaseId)
      {
        this._children[i].domContainer.style.display = "none";
      }
    }
    this._updateDisplay(true);
  },
  
  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _caseToggleSelect_changeHandler: function(event)
  {
    event.stopPropagation();
    var t = this._getCaseToggleTriggerByCaseId(event.target.value);
    t.fire();
  }
});

/** */
alfresco.xforms.CaseGroup = alfresco.xforms.VGroup.extend({
  initialize: function(xform, xformsNode)
  {
    this.parent(xform, xformsNode);
  }
});

/** 
 * Handles xforms widget xf:group for the root group.  Does some special rendering
 * to present a title rather than a group header.
 */
alfresco.xforms.ViewRoot = alfresco.xforms.VGroup.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode);
    this.focusedRepeat = null;
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods & properties
  /////////////////////////////////////////////////////////////////

  _isIndented: function()
  {
    return false;
  },

  render: function(attach_point)
  {
    this.domNode.widget = this;
    this.domNode.style.position = "relative";
    this.domNode.style.width = "100%";
    this.domNode.addClass("xformsViewRoot");

    this._groupHeaderNode = new Element("div",
                                        { 
                                          "id": this.id + "-groupHeaderNode", 
                                          "class": "xformsViewRootHeader"
                                        });
    this.domNode.appendChild(this._groupHeaderNode);

    var icon = document.createElement("img");
    this._groupHeaderNode.appendChild(icon);
    icon.setAttribute("src", alfresco.constants.WEBAPP_CONTEXT + "/images/icons/file_large.gif");
    icon.align = "absmiddle";
    icon.style.margin = "0px 5px";
    this._groupHeaderNode.appendChild(document.createTextNode(this.getLabel()));
    attach_point.appendChild(this.domNode);

    this.domNode.childContainerNode = new Element("div",
                                                  {
                                                    "id": this.id + "-childContainerNode",
                                                    "styles": {"position": "relative", "width": "100%"}
                                                  });
    return this.domNode;
  },
    
  /** */
  getLabel: function()
  {
    return this.parent() + " " + alfresco.xforms.constants.FORM_INSTANCE_DATA_NAME;
  }
});

/** A struct for providing repeat index data. */
alfresco.xforms.RepeatIndexData = function(repeat, index)
{
  this.repeat = repeat;
  this.index = index;
  this.toString = function()
  {
    return "{" + this.repeat.id + " = " + this.index + "}";
  };
}

/** 
 * Handles xforms widget xf:repeat.
 */
alfresco.xforms.Repeat = alfresco.xforms.VGroup.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode);
    this._repeatControls = [];
    this._selectedIndex = -1;
  },

  /////////////////////////////////////////////////////////////////
  // methods & properties
  /////////////////////////////////////////////////////////////////

  /** 
   * Indicates whether or not this repeat can insert more children based
   * on the alf:maximum restriction.
   */
  isInsertRepeatItemEnabled: function()
  {
    var maximum = this.xform.getBinding(this.xformsNode).maximum;
    maximum = isNaN(maximum) ? Number.MAX_VALUE : maximum;
    return this._children.length < maximum;
  },

  /** 
   * Indicates whether or not this repeat can removed children based
   * on the alf:minimum restriction.
   */
  isRemoveRepeatItemEnabled: function()
  {
    var minimum = this.xform.getBinding(this.xformsNode).minimum;
    minimum = isNaN(minimum) ? this.isRequired() ? 1 : 0 : minimum;
    return this._children.length > minimum;
  },

  /** 
   * Returns the currently selected index or -1 if this repeat has no repeat items.
   */
  getSelectedIndex: function()
  {
    this._selectedIndex = Math.min(this._children.length, this._selectedIndex);
    if (this._children.length == 0)
    {
      this._selectedIndex = -1;
    }
    return this._selectedIndex;
  },

  /** 
   * Helper function to locate the appropriate repeat item trigger for this repeat.
   * This is done by locating all related widgets via binding, and selecting the
   * Trigger who's action type is the type provided and where the properties
   * provided are the same for that action.  This approach is used rather than simply
   * looking up the trigger by id since the id isn't known for nested repeats as 
   * chiba modifies them.
   */
  _getRepeatItemTrigger: function(type, properties)
  {
    var bw = this.xform.getBinding(this.xformsNode).widgets;
    for (var i in bw)
    {
      if (! (bw[i] instanceof alfresco.xforms.Trigger))
      {
        continue;
      }

      var action = bw[i].getActions()[type];
      if (!action)
      {
        continue;
      }

      var propertiesEqual = true;
      for (var p in properties)
      {
        if (!(p in action.properties) || 
            action.properties[p] != properties[p])
        {
          propertiesEqual = false;
          break;
        }
      }
      if (propertiesEqual)
      {
        return bw[i];
      }
    }
    throw new Error("unable to find trigger " + type + 
                    ", properties " + properties +
                    " for " + this.id);

  },

  /** 
   * Sets the currently selected child by calliing XFormsBean.setRepeatIndeces.
   * If the child provided is null, the index is set to 0.
   */
  setFocusedChild: function(child)
  {
    var oldFocusedRepeat = this.getViewRoot().focusedRepeat;
    this.getViewRoot().focusedRepeat = this;
    if (oldFocusedRepeat != null && oldFocusedRepeat != this)
    {
      if (!oldFocusedRepeat.isAncestorOf(this))
      {
        oldFocusedRepeat._selectedIndex = -1;
      }
      oldFocusedRepeat._updateDisplay(false);
    }

    var repeatIndices = this.getRepeatIndices();
    if (!child)
    {
      repeatIndices.push(new alfresco.xforms.RepeatIndexData(this, 0));
      this.xform.setRepeatIndeces(repeatIndices);
    }
    else 
    {
      var index = this.getChildIndex(child);
      if (index < 0)
      {
        throw new Error("unable to find child " + child.id + " in " + this.id);
      }
  
      repeatIndices.push(new alfresco.xforms.RepeatIndexData(this, index + 1));
      // xforms repeat indexes are 1-based
      this.xform.setRepeatIndeces(repeatIndices);
    }
  },

  /** 
   * Calls swapRepeatItems on the XFormsBean which will produce the event log
   * to insert and remove the appropriate repeat items.
   */
  _swapChildren: function(fromIndex, toIndex)
  {
    alfresco.log(this.id + ".swapChildren(" + fromIndex + ", " + toIndex + ")");
    var fromChild = this.getChildAt(fromIndex);
    var toChild = this.getChildAt(toIndex);
    this.xform.swapRepeatItems(fromChild, toChild);
    var anim = dojo.lfx.html.fadeOut(fromChild.domContainer, 500);
    anim.onEnd = function()
      {
        fromChild.domContainer.style.display = "none";
      };
    anim.play();
  },

  /** 
   * Updates the repeat controls by changing the opacity on the image based on 
   * whether or not the action is enabled.
   */
  _updateRepeatControls: function()
  {
    var insertEnabled = this.isInsertRepeatItemEnabled();
    var removeEnabled = this.isRemoveRepeatItemEnabled();
    for (var i = 0; i < this._repeatControls.length; i++)
    {
      this._repeatControls[i].moveRepeatItemUpImage.setOpacity(i == 0 ? .3 : 1);
      this._repeatControls[i].moveRepeatItemDownImage.setOpacity(i == this._repeatControls.length - 1 ? .3 : 1);
      this._repeatControls[i].insertRepeatItemImage.setOpacity(insertEnabled ? 1 : .3);
      this._repeatControls[i].removeRepeatItemImage.setOpacity(removeEnabled ? 1 : .3);
    }
  },

  /////////////////////////////////////////////////////////////////
  // overridden methods & properties
  /////////////////////////////////////////////////////////////////

  /** When debugging, insert the id into the label. */
  getLabel: function()
  {
    var label = this.parentWidget.getLabel();
    if (alfresco.constants.DEBUG)
    {
      label += "[" + this.id + "]";
    }
    return label;
  },

  /** Overrides _insertChildAt in Group to provide repeater controls. */
  _insertChildAt: function(child, position)
  {
    this._repeatControls.splice(position, 0, new Element("div"));
    var images = 
      [ 
        { name: "insertRepeatItemImage", src: "plus", action: this._insertRepeatItemAfter_handler },
        { name: "moveRepeatItemUpImage", src: "arrow_up", action: this._moveRepeatItemUp_handler },
        { name: "moveRepeatItemDownImage", src: "arrow_down", action: this._moveRepeatItemDown_handler }, 
        { name: "removeRepeatItemImage", src: "minus", action: this._removeRepeatItem_handler }
      ];
    var _repeatControlsWidth = 0;
    for (var i = 0; i < images.length; i++)
    {
      var img = new Element("img", 
                            { 
                              "src": (alfresco.constants.WEBAPP_CONTEXT + "/images/icons/" + 
                                      images[i].src + ".gif"),
                              "styles": { "width" : "16px", "height" : "16px" } 
                            });
      this._repeatControls[position][images[i].name] = img;
      var imgMargin = [2, 5, 2, (i == 0 ? 5 : 0) ];
      img.style.margin = imgMargin.join("px ") + "px";
      _repeatControlsWidth += (parseInt(img.style.width) + imgMargin[1] + imgMargin[3]);
      this._repeatControls[position].appendChild(img);
      img.onclick = images[i].action.bindAsEventListener(this);
    }

    var result = this.parent(child, position);
    child.repeat = this;
    result.onclick = function(event)
    {
      event = new Event(event);
      child.repeat.setFocusedChild(child);
      event.stopPropagation();
    };
    result.addClass("xformsRepeatItem");
    if (result.nextSibling)
    {
      result.parentNode.insertBefore(this._repeatControls[position], 
                                     result.nextSibling);
    }
    else
    {
      result.parentNode.appendChild(this._repeatControls[position]);
    }

    this._repeatControls[position].addClass("xformsRepeatControls");
    this._repeatControls[position].style.width = _repeatControlsWidth + "px";
    this._repeatControls[position].style.backgroundColor = result.getStyle("background-color");
    this._repeatControls[position].style.overflow = "hidden";

    result.style.paddingBottom = (.5 * this._repeatControls[position].offsetHeight) + "px";

    this._repeatControls[position].style.top = -((.5 * this._repeatControls[position].offsetHeight) +
                                                 result.getStyle("margin-bottom").toInt() +
                                                 result.getStyle("border-bottom").toInt()) + "px";
    // may need to use this for centering repeat controls in quirks mode on IE
    // this._repeatControls[position].style.margin = "0px " + Math.floor(100 * ((result.offsetWidth - 
    // this._repeatControls[position].offsetWidth) / 
    // (result.offsetWidth * 2)))+ "%";
    return result;
  },

  /** 
   * Overrides _removeChildAt in Group to remove the repeat controls associated with
   * the repeat item.
   */
  _removeChildAt: function(position)
  {
    this._repeatControls[position].style.display = "none";
    this._repeatControls[position].remove();
    this._repeatControls.splice(position, 1);
    return this.parent(position);
  },

  /** Disables insert before. */
  _childAdded: function(child)
  {
    this.headerInsertRepeatItemImage.setOpacity(.3);
    this._updateRepeatControls();
  },

  /** Reenables insert before if there are no children left. */
  _childRemoved: function(child)
  {
    if (this._children.length == 0)
    {
      this.headerInsertRepeatItemImage.setOpacity(1);
    }
    this._updateRepeatControls();
  },

  _isIndented: function()
  {
    return false;
  },

  render: function(attach_point)
  {
    this.domNode = this.parent(attach_point);
    this.domNode.addClass("xformsRepeat");

    // clear the border bottom for the group header since we'll be getting it
    // from the repeat item border
    this._groupHeaderNode.style.borderBottomWidth = "0px";

    this._groupHeaderNode.repeat = this;
    this._groupHeaderNode.onclick = function(event)
      {
        if (event.target == event.currentTarget)
        {
          event.currentTarget.repeat.setFocusedChild(null);
        }
      };
  
    this.headerInsertRepeatItemImage = 
      new Element("img",
                  { 
                    "align": "absmiddle",
                    "src": alfresco.constants.WEBAPP_CONTEXT + "/images/icons/plus.gif",
                    "styles": { "margin-left": "5px", "width": "16px", "height": "16px" }
                  });

    this.headerInsertRepeatItemImage.repeat = this;
    this._groupHeaderNode.appendChild(this.headerInsertRepeatItemImage);

    this.headerInsertRepeatItemImage.onclick = 
      this._headerInsertRepeatItemBefore_handler.bindAsEventListener(this);

    return this.domNode;
  },

  _updateDisplay: function(recursively)
  {
    this.parent(recursively);
    if (this.getViewRoot().focusedRepeat != null &&
        (this.getViewRoot().focusedRepeat == this ||
         this.getViewRoot().focusedRepeat.isAncestorOf(this)))
    {
      this._groupHeaderNode.addClass("xformsRepeatFocusedHeader");
    }
    else 
    {
      this._groupHeaderNode.removeClass("xformsRepeatFocusedHeader");
    }

    for (var i = 0; i < this._children.length; i++)
    {
      var domContainerClasses = this._children[i].domContainer.getProperty("class").split(" ");
      if (i + 1 == this.getSelectedIndex() && this.getViewRoot().focusedRepeat == this)
      {
        domContainerClasses.remove("xformsRowOdd");
        domContainerClasses.remove("xformsRowEven");
        domContainerClasses.include("xformsRepeatItemSelected");
      }
      else
      {
        domContainerClasses.remove("xformsRepeatItemSelected");
        domContainerClasses.remove("xformsRow" + (i % 2 ? "Odd" : "Even"));
        domContainerClasses.include("xformsRow" + (i % 2 ? "Even" : "Odd"));
      }
      this._children[i].domContainer.setProperty("class", domContainerClasses.join(" "));

      this._repeatControls[i].style.backgroundColor = 
        this._children[i].domContainer.getStyle("background-color");
    }
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  /** 
   * Event handler for insert after.  If insert is enabled, causes a setRepeatIndeces
   * and an insert.
   */
  _insertRepeatItemAfter_handler: function(event)
  {
    event = new Event(event);
    event.stopPropagation();
    if (this.isInsertRepeatItemEnabled())
    {
      var index = this._repeatControls.indexOf(event.target.parentNode);
      var repeatItem = this.getChildAt(index);
      this.setFocusedChild(repeatItem);
      var trigger = this._getRepeatItemTrigger("insert", { position: "after" });
      trigger.fire();
    }
  },

  /** 
   * Event handler for insert before.  If insert is enabled, causes a setRepeatIndeces
   * and an insert.
   */
  _headerInsertRepeatItemBefore_handler: function(event)
  {
    event = new Event(event);
    if (this._children.length == 0)
    {
      event.stopPropagation();
      if (this.isInsertRepeatItemEnabled())
      {
        this.setFocusedChild(null);
        var trigger = this._getRepeatItemTrigger("insert", { position: "before" });
        trigger.fire();
      }
    }
  },

  /** 
   * Event handler for remove.  If remove is enabled, causes a setRepeatIndeces
   * and an delete.
   */
  _removeRepeatItem_handler: function(event)
  {
    event = new Event(event);
    event.stopPropagation();
    if (this.isRemoveRepeatItemEnabled())
    {
      var index = this._repeatControls.indexOf(event.target.parentNode);
      var repeatItem = this.getChildAt(index);
      this.setFocusedChild(repeatItem);
      var trigger = this._getRepeatItemTrigger("delete", {});
      trigger.fire();
    }
  },

  /** 
   * Event handler for move up.  Calls swap children with the child before
   * if the current select child is not the first child.
   */
  _moveRepeatItemUp_handler: function(event)
  {
    event = new Event(event);
    event.stopPropagation();
    var index = this._repeatControls.indexOf(event.target.parentNode);
    if (index != 0 && this._children.length != 1)
    {
      var repeatItem = this.getChildAt(index);
      this.setFocusedChild(repeatItem);
      this._swapChildren(index, index - 1);
    }
  },

  /** 
   * Event handler for move down.  Calls swap children with the child after
   * if the current select child is not the last child.
   */
  _moveRepeatItemDown_handler: function(event)
  {
    event = new Event(event);
    event.stopPropagation();
    var index = this._repeatControls.indexOf(event.target.parentNode);
    if (index != this._children.length - 1 && this._children.length != 1)
    {
      var repeatItem = this.getChildAt(index);
      this.setFocusedChild(repeatItem);
      this._swapChildren(index, index + 1);
    }
  },

  /////////////////////////////////////////////////////////////////
  // XForms event handlers
  /////////////////////////////////////////////////////////////////

  /** Sets the selected index. */
  handleIndexChanged: function(index)
  {
    alfresco.log(this.id + ".handleIndexChanged(" + index + ")");
    this._selectedIndex = index;
    this._updateDisplay(false);
  },

  /** Returns a clone of the specified prototype id. */
  handlePrototypeCloned: function(prototypeId)
  {
    alfresco.log(this.id + ".handlePrototypeCloned("+ prototypeId +")");
    var chibaData = _getElementsByTagNameNS(this.xformsNode, 
                                            alfresco.xforms.constants.CHIBA_NS,
                                            alfresco.xforms.constants.CHIBA_PREFIX,
                                            "data");
    chibaData = chibaData[chibaData.length - 1];
    var prototypeToClone = dojo.dom.firstElement(chibaData);
    if (prototypeToClone.getAttribute("id") != prototypeId)
    {
      throw new Error("unable to locate " + prototypeId +
                      " in " + this.id);
    }
    return prototypeToClone.cloneNode(true);
  },

  /** Inserts the clonedPrototype at the specified position. */
  handleItemInserted: function(clonedPrototype, position)
  {
    alfresco.log(this.id + ".handleItemInserted(" + clonedPrototype.nodeName +
               ", " + position + ")");
    var w = this.xform.createWidget(clonedPrototype);
    this._insertChildAt(w, position);
    this.xform.loadWidgets(w.xformsNode, w);
  },

  /** Deletes the item at the specified position. */
  handleItemDeleted: function(position)
  {
    alfresco.log(this.id + ".handleItemDeleted(" + position + ")");
    this._removeChildAt(position);
  }
});

////////////////////////////////////////////////////////////////////////////////
// trigger widgets
////////////////////////////////////////////////////////////////////////////////

/** 
 * Handles xforms widget xf:trigger.
 */
alfresco.xforms.Trigger = alfresco.xforms.Widget.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode, new Element("input", { type: "submit" }));
  },

  /////////////////////////////////////////////////////////////////
  // methods & properties
  /////////////////////////////////////////////////////////////////

  /** TODO: DOCUMENT */
  getActions: function()
  {
    if (typeof this._actions == "undefined")
    {
      var actionNode = _getElementsByTagNameNS(this.xformsNode, 
                                               alfresco.xforms.constants.XFORMS_NS,
                                               alfresco.xforms.constants.XFORMS_PREFIX,
                                               "action")[0];
      this._actions = {};
      for (var i = 0; i < actionNode.childNodes.length; i++)
      {
        if (actionNode.childNodes[i].nodeType != document.ELEMENT_NODE)
        {
          continue;
        }

        var a = new alfresco.xforms.XFormsAction(this.xform, actionNode.childNodes[i]);
        this._actions[a.getType()] = a;
      }
    }
    return this._actions;
  },
  
  /** fires the xforms action associated with the trigger */
  fire: function(asynchronous)
  {
    this.xform.fireAction(this.id, asynchronous);
  },
    
  /////////////////////////////////////////////////////////////////
  // overridden methods
  /////////////////////////////////////////////////////////////////

  isValidForSubmit: function()
  {
    return true;
  },

  isVisible: function()
  {
    return false;
  },

  render: function(attach_point)
  {
    attach_point.appendChild(this.domNode);
    this.widget = this.domNode;
    this.widget.value = this.getLabel() + " " + this.id;
    this.widget.onclick = this._clickHandler.bindAsEventListener(this);
    this.domContainer.style.display = "none";
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////
  _clickHandler: function(event)
  {
    this.fire();
  }
});

/** 
 * Handles xforms widget xf:submit.
 */
alfresco.xforms.Submit = alfresco.xforms.Trigger.extend({
  initialize: function(xform, xformsNode) 
  {
    this.parent(xform, xformsNode);
    var submit_buttons = (this.id == "submit" 
                          ? _xforms_getSubmitButtons()
                          : (this.id == "save-draft"
                             ? _xforms_getSaveDraftButtons()
                             : null));
    if (submit_buttons == null)
    {
      throw new Error("unknown submit button " + this.id);
    }
    submit_buttons.each(function(b)
                        {
                          alfresco.log("adding submit handler for " + b.getAttribute('id'));
                          $(b).onclick = this._submitButton_clickHandler.bindAsEventListener(this);
                        }.bind(this));
  },

  /////////////////////////////////////////////////////////////////
  // DOM event handlers
  /////////////////////////////////////////////////////////////////

  _clickHandler: function(event)
  {
    this.done = false;
    _hide_errors();
    this.fire();
  },

  /** */
  _submitButton_clickHandler: function(event)
  {
    event = new Event(event);
    var result;
    if (this.xform.submitWidget && this.xform.submitWidget.done)
    {
      alfresco.log("done - doing base click on " + this.xform.submitWidget.currentButton.id);
      this.xform.submitWidget.currentButton = null;
      this.xform.submitWidget = null;
      result = true;
    }
    else
    {
      alfresco.log("triggering submit from handler " + event.target.id);
      event.stopPropagation();
      _hide_errors();
      this.xform.submitWidget = this;
      this.xform.submitWidget.currentButton = event.target;
      this.xform.submitWidget.fire(true);
      result = false;
    }
    alfresco.log("submit click handler exit " + event.target.id + " with result " + result);
    return result;
  }
});

/**
 * A struct describing an xforms action block.
 */
alfresco.xforms.XFormsAction = new Class({
  initialize: function(xform, xformsNode)
  {
    this.xform = xform;
    this.xformsNode = xformsNode;
    /** All properties of the action as map of key value pairs */
    this.properties = [];
    for (var i = 0; i < this.xformsNode.attributes.length; i++)
    {
      var attr = this.xformsNode.attributes[i];
      if (attr.nodeName.match(new RegExp("^" + alfresco.xforms.constants.XFORMS_PREFIX + ":")))
      {
        this.properties[attr.nodeName.substring((alfresco.xforms.constants.XFORMS_PREFIX + ":").length)] = 
          attr.nodeValue;
      }
    }
    if (this.getType() == "setvalue" && !this.properties["value"])
    {
      this.properties["value"] = this.xformsNode.firstChild.nodeValue;
    }
  },

  /** Returns the action type. */
  getType: function()
  {
    return this.xformsNode.nodeName.substring((alfresco.xforms.constants.XFORMS_PREFIX + ":").length);
  }
});

////////////////////////////////////////////////////////////////////////////////
// xforms data model
////////////////////////////////////////////////////////////////////////////////

/** 
 * An xforms event.  A log of events is returned by any xforms action and 
 * is used to update the UI appropriately.
 */
alfresco.xforms.XFormsEvent = new Class({
  initialize: function(node)
  {
    this.type = node.nodeName;
    this.targetId = node.getAttribute("targetId");
    this.targetName = node.getAttribute("targetName");
    this.properties = {};
    for (var i = 0; i < node.childNodes.length; i++)
    {
      if (node.childNodes[i].nodeType == document.ELEMENT_NODE)
      {
        this.properties[node.childNodes[i].getAttribute("name")] =
          node.childNodes[i].getAttribute("value");
      }
    }
  },

  /** Returns the widget managing the specified target id. */
  getTarget: function()
  {
    var targetDomNode = document.getElementById(this.targetId + "-content");
    if (!targetDomNode)
    {
      throw new Error("unable to find node " + this.targetId + "-content");
    }
    return targetDomNode.widget;
  }
});

/**
 * A parsed xf:bind.
 */
alfresco.xforms.Binding = new Class({
  initialize: function(xformsNode, parentBinding)
    {
      this.xformsNode = xformsNode;
      this.id = this.xformsNode.getAttribute("id");
      this.nodeset =  this.xformsNode.getAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":nodeset");
      this._readonly =
        (_hasAttribute(this.xformsNode, alfresco.xforms.constants.XFORMS_PREFIX + ":readonly")
         ? this.xformsNode.getAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":readonly") == "true()"
         : null);
      this._required =
        (_hasAttribute(this.xformsNode, alfresco.xforms.constants.XFORMS_PREFIX + ":required")
         ? this.xformsNode.getAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":required") == "true()"
         : null);
      this._type =
        (_hasAttribute(this.xformsNode, alfresco.xforms.constants.XFORMS_PREFIX + ":type")
         ? this.xformsNode.getAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":type")
         : null);
      this._builtInType =
        (_hasAttribute(this.xformsNode, alfresco.xforms.constants.ALFRESCO_PREFIX + ":builtInType")
         ? this.xformsNode.getAttribute(alfresco.xforms.constants.ALFRESCO_PREFIX + ":builtInType")
         : null);
      this.constraint = 
        (_hasAttribute(this.xformsNode, alfresco.xforms.constants.XFORMS_PREFIX + ":constraint")
         ? this.xformsNode.getAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":constraint")
         : null);
      this.maximum = this.xformsNode.getAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":maxOccurs");
      this.maximum = this.maximum == "unbounded" ? Number.MAX_VALUE : parseInt(this.maximum);
      this.minimum = parseInt(this.xformsNode.getAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":minOccurs"));
      this.parentBinding = parentBinding;
      this.widgets = {};
    },

  /** Returns the expected schema type for this binding. */
  getType: function()
  {
    return (this._type != null
            ? this._type
            : (this.parentBinding != null ? this.parentBinding.getType() : null));
  },

  /** Returns the expected built in schema type for this binding. */
  getBuiltInType: function()
  {
    return (this._builtInType != null
            ? this._builtInType
            : (this.parentBinding != null ? this.parentBinding.getBuiltInType() : null));
  },

  /** Returns true if a node bound by this binding has a readonly value */
  isReadonly: function()
  {
    return (this._readonly != null ? this._readonly : 
            (this.parentBinding != null ? this.parentBinding.isReadonly() : false));
  },
    
  /** Returns true if a node bound by this binding has a required value */
  isRequired: function()
  {
    return (this._required != null ? this._required :
            (this.parentBinding != null ? this.parentBinding.isRequired() : false));
  },
  
  toString: function()
  {
    return ("{id: " + this.id + 
            ",type: " + this.getType() + 
            ",builtInType: " + this.getBuiltInType() + 
            ",required: " + this.isRequired() +
            ",readonly: " + this.isReadonly() +
            ",nodeset: " + this.nodeset + "}");
  }
});

/**
 * Manages the xforms document.
 */
alfresco.xforms.XForm = new Class({

  /** Makes a request to the XFormsBean to load the xforms document. */
  initialize: function()
  {
    alfresco.AjaxHelper.sendRequest("XFormsBean.getXForm",
                                    null,
                                    true,
                                    this._loadHandler.bindAsEventListener(this));
  },

  /////////////////////////////////////////////////////////////////
  // Initialization
  /////////////////////////////////////////////////////////////////

  /** Parses the xforms document and produces the widget tree. */
  _loadHandler: function(xformDocument)
  {
    this.xformDocument = xformDocument;
    this.xformsNode = xformDocument.documentElement;
    this._bindings = this._loadBindings(this.getModel());
  
    var bindings = this.getBindings();
    var alfUI = document.getElementById(alfresco.xforms.constants.XFORMS_UI_DIV_ID);
    alfUI.style.width = "100%";
    var rootGroup = _getElementsByTagNameNS(this.getBody(),
                                            alfresco.xforms.constants.XFORMS_NS,
                                            alfresco.xforms.constants.XFORMS_PREFIX,
                                            "group")[0];

    this.rootWidget = new alfresco.xforms.ViewRoot(this, rootGroup);
    this.rootWidget.render(alfUI);

    this.loadWidgets(rootGroup, this.rootWidget);
  },

  /** Creates the widget for the provided xforms node. */
  createWidget: function(xformsNode)
  {
    var appearance = (xformsNode.getAttribute("appearance") ||
                      xformsNode.getAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":appearance"));
    appearance = appearance == null || appearance.length == 0 ? null : appearance;

    var xformsType = xformsNode.nodeName.toLowerCase();
    var binding = this.getBinding(xformsNode);
    var schemaType = binding ? binding.getType() : null;
    var builtInSchemaType = binding ? binding.getBuiltInType() : null;

    alfresco.log("creating widget for xforms type " + xformsType +
               " schema type " + schemaType +
               " built in schema type " + builtInSchemaType +
               " with appearance " + appearance);
    var x = alfresco.xforms.widgetConfig[xformsType];
    if (!x)
    {
      throw new Error("unknown type " + xformsNode.nodeName);
    }
    x = schemaType in x ? x[schemaType] : builtInSchemaType in x ? x[builtInSchemaType] : x["*"];
    x = appearance in x ? x[appearance] : x["*"];
    // alfresco.log(xformsType + ":" + schemaType + ":" + appearance + " =>" + x);
    if (x === undefined)
    {
      throw new Error("unable to find widget for xforms type " + xformsType +
                      " schemaType " + schemaType +
                      " appearance " + appearance);
    }
    if (x == null || typeof x.className == "undefined")
    {
      return null;
    }
    var cstr = eval(x.className);
    if (!cstr)
    {
      throw new Error("unable to load constructor " + x.className +
                      " for xforms type " + xformsType +
                      " schemaType " + schemaType +
                      " appearance " + appearance);
    }
    var result = new cstr(this, xformsNode, $merge({}, x.params));
    if (result instanceof alfresco.xforms.Widget)
    {
      return result;
    }
    else
    {
      throw new Error("constructor for widget " + x + 
                      " for xforms type " + xformsType +
                      " schemaType " + schemaType +
                      " appearance " + appearance +
                      " is not an alfresco.xforms.Widget");
    }
  },

  /** Loads all widgets for the provided xforms node's children. */
  loadWidgets: function(xformsNode, parentWidget)
  {
    for (var i = 0; i < xformsNode.childNodes.length; i++)
    {
      if (xformsNode.childNodes[i].nodeType != document.ELEMENT_NODE)
      {
        continue;
      }
      alfresco.log("loading " + xformsNode.childNodes[i].nodeName + 
                 " into " + parentWidget.id);
      if (xformsNode.childNodes[i].getAttribute(alfresco.xforms.constants.ALFRESCO_PREFIX +
                                                ":prototype") == "true")
      {
        alfresco.log(xformsNode.childNodes[i].getAttribute("id") + 
                   " is a prototype, ignoring");
        continue;
      }
      var w = this.createWidget(xformsNode.childNodes[i]);
      if (w != null)
      {
        alfresco.log("created " + w.id + " for " + xformsNode.childNodes[i].nodeName);
        parentWidget.addChild(w);
        if (w instanceof alfresco.xforms.AbstractGroup)
        {
          this.loadWidgets(xformsNode.childNodes[i], w);
        }
      }
    }
  },

  /** Loads all bindings from the xforms document. */
  _loadBindings: function(bind, parentBinding, result)
  {
    result = result || [];
    for (var i = 0; i < bind.childNodes.length; i++)
    {
      if (bind.childNodes[i].nodeName.toLowerCase() == 
          alfresco.xforms.constants.XFORMS_PREFIX + ":bind")
      {
        var b = new alfresco.xforms.Binding(bind.childNodes[i], parentBinding);
        result[b.id] = b;
        alfresco.log("loaded binding " + b);
        this._loadBindings(bind.childNodes[i], result[b.id], result);
      }
    }
    return result;
  },

  /////////////////////////////////////////////////////////////////
  // XForms model properties & methods
  /////////////////////////////////////////////////////////////////

  /** Returns the model section of the xforms document. */
  getModel: function()
  {
    return _getElementsByTagNameNS(this.xformsNode, 
                                   alfresco.xforms.constants.XFORMS_NS, 
                                   alfresco.xforms.constants.XFORMS_PREFIX, 
                                   "model")[0];
  },

  /** Returns the instance section of the xforms document. */
  getInstance: function()
  {
    return _getElementsByTagNameNS(this.getModel(),
                                   alfresco.xforms.constants.XFORMS_NS,
                                   alfresco.xforms.constants.XFORMS_PREFIX,
                                   "instance")[0];
  },

  /** Returns the body section of the xforms document. */
  getBody: function()
  {
    var b = _getElementsByTagNameNS(this.xformsNode,
                                    alfresco.xforms.constants.XHTML_NS,
                                    alfresco.xforms.constants.XHTML_PREFIX,
                                    "body");
    return b[b.length - 1];
  },

  /** Returns the binding corresponding to the provided xforms node. */
  getBinding: function(xformsNode)
  {
    return this._bindings[xformsNode.getAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":bind")];
  },

  /** Returns all parsed bindings. */
  getBindings: function()
  {
    return this._bindings;
  },

  /////////////////////////////////////////////////////////////////
  // XFormsBean interaction
  /////////////////////////////////////////////////////////////////

  /** swaps the specified repeat items by calling XFormsBean.swapRepeatItems. */
  swapRepeatItems: function(fromChild, toChild)
  {
    var params = 
    {
      fromItemId: fromChild.xformsNode.getAttribute("id"),
      toItemId: toChild.xformsNode.getAttribute("id"),
      instanceId: this.getInstance().getAttribute("id")
    };

    alfresco.AjaxHelper.sendRequest("XFormsBean.swapRepeatItems",
                                    params,
                                    false,
                                    this._handleEventLog.bindAsEventListener(this));
  },

  /** sets the repeat indexes by calling XFormsBean.setRepeatIndeces. */
  setRepeatIndeces: function(repeatIndeces)
  {
    alfresco.log("setting repeat indeces [" + repeatIndeces.join(", ") + "]");
    var params = { };
    params["repeatIds"] = [];
    for (var i = 0; i < repeatIndeces.length; i++)
    {
      params.repeatIds.push(repeatIndeces[i].repeat.id);
      params[repeatIndeces[i].repeat.id] = repeatIndeces[i].index;
    }
    params.repeatIds = params.repeatIds.join(",");
    alfresco.AjaxHelper.sendRequest("XFormsBean.setRepeatIndeces",
                                    params,
                                    false,
                                    this._handleEventLog.bindAsEventListener(this));
  },

  /** Fires an action specified by the id by calling XFormsBean.fireAction. */
  fireAction: function(id, asynchronous)
  {
    alfresco.log("fireAction(" + id + ")");
    alfresco.AjaxHelper.sendRequest("XFormsBean.fireAction",
                                    { id: id },
                                    $pick(asynchronous, false),
                                    this._handleEventLog.bindAsEventListener(this));
  },

  /** Sets the value of the specified control id by calling XFormsBean.setXFormsValue. */
  setXFormsValue: function(id, value)
  {
    value = value == null ? "" : value.toString();
    alfresco.log("setting value " + id + " = " + value);
    alfresco.AjaxHelper.sendRequest("XFormsBean.setXFormsValue",
                                    { id: id, value: value },
                                    true,
                                    this._handleEventLog.bindAsEventListener(this));
  },

  /** Handles the xforms event log resulting from a call to the XFormsBean. */
  _handleEventLog: function(events)
  {
    events = events.documentElement;
    var prototypeClones = [];
    var generatedIds = null;
    for (var i = 0; i < events.childNodes.length; i++)
    {
      if (events.childNodes[i].nodeType != document.ELEMENT_NODE)
      {
        continue;
      }
      var xfe = new alfresco.xforms.XFormsEvent(events.childNodes[i]);
      alfresco.log("parsing " + xfe.type +
                 "(" + xfe.targetId + ", " + xfe.targetName + ")");
      switch (xfe.type)
      {
      case "chiba-index-changed":
      {
        var index = Number(xfe.properties["index"]);
        try
        {
          xfe.getTarget().handleIndexChanged(index);
        }
        catch (e)
        {
          alfresco.log(e);
        }
        break;
      }
      case "chiba-state-changed":
      {
        alfresco.log("handleStateChanged(" + xfe.targetId + ")");
        xfe.getTarget().setModified(true);
        if ("valid" in xfe.properties)
        {
          xfe.getTarget().setValid(xfe.properties["valid"] == "true");
        }
        if ("required" in xfe.properties)
        {
          xfe.getTarget().setRequired(xfe.properties["required"] == "true");
        }
        if ("readonly" in xfe.properties)
        {
          xfe.getTarget().setReadonly(xfe.properties["readonly"] == "true");
        }
        if ("enabled" in xfe.properties)
        {
          xfe.getTarget().setEnabled(xfe.properties["enabled"] == "true");
        }
        if ("value" in xfe.properties)
        {
          alfresco.log("setting " + xfe.getTarget().id + " = " + xfe.properties["value"]);
          xfe.getTarget().setValue(xfe.properties["value"]);
        }
        break;
      }
      case "chiba-prototype-cloned":
      {
        var prototypeId = xfe.properties["prototypeId"];
        var originalId = xfe.properties["originalId"];
        alfresco.log("handlePrototypeCloned(" + xfe.targetId + 
                   ", " + originalId + 
                   ", " + prototypeId + ")");
        var clone = null;
        var prototypeNode = _findElementById(this.xformsNode, prototypeId);
        if (prototypeNode)
        {
          alfresco.log("cloning prototype " + prototypeNode.getAttribute("id"));
          clone = prototypeNode.cloneNode(true);
        }
        else
        {
          alfresco.log("cloning prototype " + originalId);
          var prototypeNode = _findElementById(this.xformsNode, originalId);
          //clone = prototypeNode.cloneNode(true);
          clone = prototypeNode.ownerDocument.createElement(alfresco.xforms.constants.XFORMS_PREFIX + ":group");
          clone.setAttribute(alfresco.xforms.constants.XFORMS_PREFIX + ":appearance", "repeated");
          for (var j = 0; j < prototypeNode.childNodes.length; j++)
          {
            clone.appendChild(prototypeNode.childNodes[j].cloneNode(true));
          }
          clone.setAttribute("id", prototypeId);
        }

        if (clone == null)
        {
          throw new Error("unable to clone prototype " + prototypeId);
        }

        alfresco.log("created clone " + clone.getAttribute("id") + 
                   " nodeName " + clone.nodeName +
                   " parentClone " + (prototypeClones.length != 0 
                                      ? prototypeClones.peek().getAttribute("id") 
                                      : null));
        prototypeClones.push(clone);
        break;
      }
      case "chiba-id-generated":
      {
        var originalId = xfe.properties["originalId"];
  
        alfresco.log("handleIdGenerated(" + xfe.targetId + ", " + originalId + ")");
        var node = _findElementById(prototypeClones.peek(), originalId);
        if (!node)
        {
          throw new Error("unable to find " + originalId + 
                          " in clone " + dojo.dom.innerXML(clone));
        }
        alfresco.log("applying id " + xfe.targetId + 
                   " to " + node.nodeName + "(" + originalId + ")");
        node.setAttribute("id", xfe.targetId);
        generatedIds = generatedIds || new Object();
        generatedIds[xfe.targetId] = originalId;
        if (prototypeClones.length != 1)
        {
          var e = _findElementById(prototypeClones[prototypeClones.length - 2], originalId);
          if (e)
          {
            e.setAttribute(alfresco.xforms.constants.ALFRESCO_PREFIX + ":prototype", "true");
          }
        }
        break;
      }
      case "chiba-item-inserted":
      {
        var position = Number(xfe.properties["position"]) - 1;
        var originalId = xfe.properties["originalId"];
        var clone = prototypeClones.pop();
        // walk all nodes of the clone and ensure that they have generated ids.
        // those that do not are nested repeats that should not be added
        if ((clone.getAttribute("id") in generatedIds) == false)
        {
           throw new Error("expected clone id " + clone.getAttribute("id") +
                           " to be a generated id");
        }
        
        function _removeNonGeneratedChildNodes(node, ids)
        {
          var child = node.firstChild;
          while (child)
          {
            var next = child.nextSibling;
            if (child.nodeType == document.ELEMENT_NODE)
            {
              if (child.getAttribute("id") in ids)
              {
                _removeNonGeneratedChildNodes(child, ids);
              }
              else
              {
                node.removeChild(child);
              }
            }
            child = next;
          }
        };
        _removeNonGeneratedChildNodes(clone, generatedIds);

        if (prototypeClones.length != 0)
        {
          alfresco.log("using parentClone " + prototypeClones.peek().getAttribute("id") + 
                       " of " + clone.getAttribute("id"));
          var parentRepeat = _findElementById(prototypeClones.peek(), xfe.targetId);
          parentRepeat.appendChild(clone);
        }
        else
        {
          alfresco.log("no parentClone found, directly insert " + clone.getAttribute("id") +
                     " on " + xfe.targetId);
          xfe.getTarget().handleItemInserted(clone, position);
        }
        break;
      }
      case "chiba-item-deleted":
      {
        var position = Number(xfe.properties["position"]) - 1;
        xfe.getTarget().handleItemDeleted(position);
        break;
      }
      case "chiba-replace-all":
      {
        if (this.submitWidget)
        {
          this.submitWidget.done = true;
          this.submitWidget.currentButton.click();
        }
        break;
      }
      case "chiba-switch-toggled":
      {
        var switchElement = xfe.getTarget();
        switchElement.handleSwitchToggled(xfe.properties["selected"], 
                                          xfe.properties["deselected"]);
      }
      case "xforms-valid":
      {
        xfe.getTarget().setValid(true);
        xfe.getTarget().setModified(true);
        break;
      }
      case "xforms-invalid":
      {
        xfe.getTarget().setValid(false);
        xfe.getTarget().setModified(true);
        break;
      }
      case "xforms-required":
      {
        xfe.getTarget().setRequired(true);
        break;
      }
      case "xforms-optional":
      {
        xfe.getTarget().setRequired(false);
        break;
      }
      case "xforms-submit-error":
      {
        this.submitWidget = null;
        var invalid_widgets = this.rootWidget.getWidgetsInvalidForSubmit();
        _show_error(document.createTextNode(alfresco.resources["validation_provide_values_for_required_fields"]));
        var error_list = document.createElement("ul");
        invalid_widgets.each(function(invalid)
        {
          var error_item = document.createElement("li");
          error_item.appendChild(document.createTextNode(invalid.getAlert()));
          error_list.appendChild(error_item);
          invalid.showAlert();
        });
        _show_error(error_list);
        break;
      }
      case "xforms-readonly":
      {
        xfe.getTarget().setReadonly(true);
        break;
      }
      case "xforms-readwrite":
      {
        xfe.getTarget().setReadonly(false);
        break;
      }
      case "xforms-submit":
      case "xforms-submit-done":
      case "xforms-enabled":
      case "xforms-disabled":
        break;
      default:
      {
        alfresco.log("unhandled event " + events.childNodes[i].nodeName);
      }
      }
    }
  }
});

////////////////////////////////////////////////////////////////////////////////
// error message display management
////////////////////////////////////////////////////////////////////////////////

/** hides the error message display. */
function _hide_errors()
{
  var errorDiv = $(alfresco.xforms.constants.XFORMS_ERROR_DIV_ID);
  if (errorDiv)
  {
    errorDiv.empty();
    errorDiv.style.display = "none";
  }
}

/** shows the error message display. */
function _show_error(msg)
{
  var errorDiv = $(alfresco.xforms.constants.XFORMS_ERROR_DIV_ID);
  if (!errorDiv)
  {
    errorDiv = new Element("div", { "id": alfresco.xforms.constants.XFORMS_ERROR_DIV_ID });
    errorDiv.addClass("infoText statusErrorText xformsError");
    errorDiv.injectBefore($(alfresco.xforms.constants.XFORMS_UI_DIV_ID));
  }

  if (errorDiv.style.display == "block")
  {
    errorDiv.appendChild(document.createElement("br"));
  }
  else
  {
    errorDiv.style.display = "block";
  }
  errorDiv.appendChild(msg);
}

////////////////////////////////////////////////////////////////////////////////
// DOM utilities - XXXarielb should be merged into common.js
////////////////////////////////////////////////////////////////////////////////

function _findElementById(node, id)
{
//  alfresco.log("looking for " + id + 
//             " in " + (node ? node.nodeName : null) + 
//             "(" + (node ? node.getAttribute("id") : null) + ")");
  if (node.getAttribute("id") == id)
  {
    return node;
  }
  for (var i = 0; i < node.childNodes.length; i++)
  {
    if (node.childNodes[i].nodeType == document.ELEMENT_NODE)
    {
      var n = _findElementById(node.childNodes[i], id);
      if (n)
      {
        return n;
      }
    }
  }
  return null;
}

function _hasAttribute(node, name)
{
  return (node == null
          ? false
          : (node.hasAttribute
             ? node.hasAttribute(name)
             : node.getAttribute(name) != null));
}

function _getElementsByTagNameNS(parentNode, ns, nsPrefix, tagName)
{
  return (parentNode.getElementsByTagNameNS
          ? parentNode.getElementsByTagNameNS(ns, tagName)
          : parentNode.getElementsByTagName(nsPrefix + ":" + tagName));
}

////////////////////////////////////////////////////////////////////////////////
// XPath wrapper
////////////////////////////////////////////////////////////////////////////////

function _evaluateXPath(xpath, contextNode, result_type)
{
  var xmlDocument = contextNode.ownerDocument;
  if (alfresco.constants.DEBUG)
  {
    alfresco.log("evaluating xpath " + xpath +
               " on node " + contextNode.nodeName +
               " in document " + xmlDocument);
  }
  var result = null;
  if (xmlDocument.evaluate)
  {
    var nsResolver = (xmlDocument.createNSResolver 
                      ? xmlDocument.createNSResolver(xmlDocument.documentElement) 
                      : null);
    result = xmlDocument.evaluate(xpath, 
                                  contextNode, 
                                  nsResolver, 
                                  result_type,
                                  null);
    if (result)
    {
      switch (result_type)
      {
      case XPathResult.FIRST_ORDERED_NODE_TYPE:
        result = result.singleNodeValue;
        break;
      case XPathResult.BOOLEAN_TYPE:
        result = result.booleanValue;
        break;
      case XPathResult.STRING_TYPE:
        result = result.stringValue;
        break;
      }
    }
  }
  else
  {
    xmlDocument.setProperty("SelectionLanguage", "XPath");
    var namespaces = [];
    for (var i = 0; i < xmlDocument.documentElement.attributes.length; i++)
    {
      var attr = xmlDocument.documentElement.attributes[i];
      if (attr.nodeName.match(/^xmlns:/))
      {
        namespaces.push(attr.nodeName + "=\'" + attr.nodeValue + "\'");
      }
    }

    if (alfresco.constants.DEBUG)
    {
      alfresco.log("using namespaces " + namespaces.join(","));
    }
    xmlDocument.setProperty("SelectionNamespaces", namespaces.join(' '));
    if (result_type == XPathResult.FIRST_ORDERED_NODE_TYPE)
    {
      result = xmlDocument.selectSingleNode(xpath);
    }
    else if (result_type == XPathResult.BOOLEAN_TYPE)
    {
      result = true;
    }
  }
  alfresco.log("resolved xpath " + xpath + " to " + result);
  return result;
}

if (!XPathResult)
{
  var XPathResult = 
  {
    ANY_TYPE: 0,
    NUMBER_TYPE: 1,
    STRING_TYPE:  2,
    BOOEAN_TYPE: 3,
    FIRST_ORDERED_NODE_TYPE: 9
  };
}

dojo.html.toCamelCase = function(str)
{
  return str.replace(/-./, function(str) { return str.charAt(1).toUpperCase(); });
}

////////////////////////////////////////////////////////////////////////////////
// tiny mce integration
////////////////////////////////////////////////////////////////////////////////

alfresco.constants.TINY_MCE_DEFAULT_PLUGINS = 
  alfresco.xforms.RichTextEditor.determineNecessaryTinyMCEPlugins(alfresco.xforms.widgetConfig);

alfresco.constants.TINY_MCE_DEFAULT_SETTINGS = 
{
  theme: "advanced",
  mode: "exact",
  plugins: alfresco.constants.TINY_MCE_DEFAULT_PLUGINS,
  width: -1,
  height: -1,
  auto_resize: false,
  force_p_newlines: false,
  encoding: "UTF-8",
  entity_encoding: "raw",
  add_unload_trigger: false,
  add_form_submit_trigger: false,
  theme_advanced_toolbar_location: "top",
  theme_advanced_toolbar_align: "left",
  theme_advanced_buttons1: "",
  theme_advanced_buttons2: "",
  theme_advanced_buttons3: "",
  urlconverter_callback: "alfresco_TinyMCE_urlconverter_callback",
  file_browser_callback: "alfresco_TinyMCE_file_browser_callback"
};

tinyMCE.init($extend({}, alfresco.constants.TINY_MCE_DEFAULT_SETTINGS));

window.addEvent("domready", 
                function() 
                { 
                  document.xform = new alfresco.xforms.XForm(); 
                });
