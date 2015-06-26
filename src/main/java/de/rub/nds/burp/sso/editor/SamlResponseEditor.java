/**
 * BurpSSOExtension - An extension for BurpSuite that highlights SSO messages.
 * Copyright (C) 2015/ Christian Mainka
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package de.rub.nds.burp.sso.editor;

import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IMessageEditorController;
import burp.IMessageEditorTab;
import burp.IMessageEditorTabFactory;
import burp.IParameter;
import burp.ITextEditor;
import java.awt.Component;

public class SamlResponseEditor implements IMessageEditorTabFactory {

	private IBurpExtenderCallbacks callbacks;
	private IExtensionHelpers helpers;

	public SamlResponseEditor(IBurpExtenderCallbacks callbacks) {
		this.callbacks = callbacks;
		this.helpers = callbacks.getHelpers();
	}

	//
	// implement IMessageEditorTabFactory
	//
	@Override
	public IMessageEditorTab createNewInstance(IMessageEditorController controller, boolean editable) {
		// create a new instance of our custom editor tab
		return new Base64InputTab(controller, editable);
	}

	//
	// class implementing IMessageEditorTab
	//
	class Base64InputTab implements IMessageEditorTab {

		private boolean editable;
		private ITextEditor txtInput;
		private byte[] currentMessage;
		final String parameterName = "SAMLResponse";

		public Base64InputTab(IMessageEditorController controller, boolean editable) {
			this.editable = editable;

			// create an instance of Burp's text editor, to display our deserialized data
			txtInput = callbacks.createTextEditor();
			txtInput.setEditable(editable);
		}

		//
		// implement IMessageEditorTab
		//
		@Override
		public String getTabCaption() {
			return parameterName;
		}

		@Override
		public Component getUiComponent() {
			return txtInput.getComponent();
		}

		@Override
		public boolean isEnabled(byte[] content, boolean isRequest) {
			return isRequest && isSamlResponse(content);
		}

		private boolean isSamlResponse(byte[] content) {
			return null != getSamlResponse(content);
		}

		private IParameter getSamlResponse(byte[] content) {
			return helpers.getRequestParameter(content, parameterName);
		}

		@Override
		public void setMessage(byte[] content, boolean isRequest) {
			if (content == null) {
				// clear our display
				txtInput.setText(null);
				txtInput.setEditable(false);
			} else {
				// retrieve the data parameter
				IParameter parameter;
				parameter = getSamlResponse(content);

				// deserialize the parameter value
				txtInput.setText(helpers.base64Decode(helpers.urlDecode(parameter.getValue())));
				txtInput.setEditable(editable);
			}

			// remember the displayed content
			currentMessage = content;
		}

		@Override
		public byte[] getMessage() {
			// determine whether the user modified the deserialized data
			if (txtInput.isTextModified()) {
				// reserialize the data
				byte[] text = txtInput.getText();
				String input = helpers.urlEncode(helpers.base64Encode(text));

				// update the request with the new parameter value
				return helpers.updateParameter(currentMessage, helpers.buildParameter(parameterName, input, IParameter.PARAM_BODY));
			} else {
				return currentMessage;
			}
		}

		@Override
		public boolean isModified() {
			return txtInput.isTextModified();
		}

		@Override
		public byte[] getSelectedData() {
			return txtInput.getSelectedText();
		}
	}
}
