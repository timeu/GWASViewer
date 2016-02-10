package com.github.timeu.gwtlibs.gwasviewer.client.resources;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;


public interface Resources extends ClientBundle  {

	interface MainStyle extends CssResource {

		String settingsPopup();
		String settingsButton();
		String settingsContent();
		String settingsContentItemCheckboxChecked();
		String settingsContentItem();
		String settingsContentItemText();
		String settingsContentItemCheckbox();
		String settingsContentDeleteImage();
	}
	@Source("style.gss")
	MainStyle style();

	@Source("checkmark.png")
    ImageResource checkmark();

	@Source("settings_icon.png")
    ImageResource settings_icon();

	@Source("delete.png")
	ImageResource delete();
}
