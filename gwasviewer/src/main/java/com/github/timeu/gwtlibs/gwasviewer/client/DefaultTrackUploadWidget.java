package com.github.timeu.gwtlibs.gwasviewer.client;

import com.github.timeu.gwtlibs.gwasviewer.client.events.UploadTrackEvent;
import com.github.timeu.gwtlibs.gwasviewer.client.util.File;
import com.github.timeu.gwtlibs.gwasviewer.client.util.FileReader;
import com.github.timeu.gwtlibs.gwasviewer.client.util.LoadEndHandler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.gwt.charts.client.ColumnType;
import com.googlecode.gwt.charts.client.DataTable;

/**
 * Created by uemit.seren on 2/10/16.
 */
public class DefaultTrackUploadWidget extends Composite implements UploadTrackWidget {

    private static Binder uiBinder = GWT.create(Binder.class);
    @UiField
    FormPanel uploadFormPanel;
    @UiField
    SpanElement uploadErrorMessage;
    @UiField
    RadioButton saveLocalRb;
    @UiField
    RadioButton saveServerRb;
    @UiField
    FileUpload fileBox;
    @UiField
    TextBox urlTb;
    @UiField
    Button formSubmitBtn;
    @UiField
    TextBox nameTb;
    private Storage localStore = null;

    interface Binder extends UiBinder<Widget, DefaultTrackUploadWidget> {	}

    public DefaultTrackUploadWidget() {
        initWidget(uiBinder.createAndBindUi(this));
        localStore = Storage.getLocalStorageIfSupported();
        if (localStore == null) {
            saveLocalRb.setVisible(false);
            saveServerRb.setValue(true);
        }
    }


    @UiHandler("cancelUploadBtn")
    public void onClickCancel(ClickEvent e) {
        reset();
    }

    @UiHandler("formSubmitBtn")
    public void onClickSave(ClickEvent event) {
        if (saveServerRb.getValue()) {
            uploadFormPanel.submit();
        }
        else {
            if (!fileBox.getFilename().isEmpty()) {
                retrieveFileContentAndStore();
            }
            else {
                retrieveFromUrlAndStore();
            }
        }
    }

    private void retrieveFileContentAndStore() {
        FileReader reader = getFileReader();
        reader.setOnloadend(new LoadEndHandler() {
            @Override
            public void onLoadEnd() {
                if (reader.getReadyState() == 2) {
                    String fileContent = reader.getResult().toString();
                    storeInLocalStorage(fileContent);
                }
            }
        });
        reader.readAsText(getFile(fileBox.getElement()),"");
    }

    private final native FileReader getFileReader() /*-{
         return new FileReader();
    }-*/;

    private final native File getFile(Element element)/*-{
         return element.files[0];
    }-*/;

    private void retrieveFromUrlAndStore() {
        RequestBuilder request = new RequestBuilder(RequestBuilder.GET,urlTb.getText());
        try {
            request.sendRequest("", new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == 200) {
                        storeInLocalStorage(response.getText());
                    }
                    else {
                        uploadErrorMessage.setInnerHTML(response.getText());
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    uploadErrorMessage.setInnerText(exception.getMessage());
                }
            });
        } catch (RequestException e) {
            uploadErrorMessage.setInnerText(e.getMessage());
        }
    }

    @UiHandler({"urlTb","nameTb"})
    public void onChangeUrl(KeyUpEvent e) {
        updateFormSubmitBtn();
    }

    @UiHandler("fileBox")
    public void onChangeFileBox(ChangeEvent e) {
        updateFormSubmitBtn();
    }

    @UiHandler("uploadFormPanel")
    public void onFormSubmit(FormPanel.SubmitCompleteEvent e) {
        reset();
        fireEvent(new UploadTrackEvent(e.getResults()));
    }

    private void storeInLocalStorage(String data) {
        DataTable dataTable = createDataTableFromString(data);
        if (dataTable == null) {
            uploadErrorMessage.setInnerText("Error parsing data");
            return;
        }
        String json = dataTable.toJSON();
        String id = dataTable.getColumnId(2);
        localStore.setItem("gwasviewerstats_"+ id,json);
        fireEvent(new UploadTrackEvent(null));
        reset();
    }

    private DataTable createDataTableFromString(String data) {
        String[] rows = data.split("\n");
        String[] header = rows[0].split(",");
        if (header.length != 3)
            return null;
        DataTable dataTable = DataTable.create();
        dataTable.addColumn(ColumnType.STRING,header[0]);
        dataTable.addColumn(ColumnType.NUMBER,header[1]);
        dataTable.addColumn(ColumnType.NUMBER,nameTb.getText(),header[2]);
        for (int i=1;i<rows.length;i++) {
            String[] values = rows[i].split(",");
            int rowidx = dataTable.addRow();
            dataTable.setCell(rowidx,0,values[0]);
            dataTable.setCell(rowidx,1,Integer.valueOf(values[1]));
            dataTable.setCell(rowidx,2,Double.valueOf(values[2]));
        }
        return dataTable;
    }

    private void reset() {
        uploadFormPanel.reset();
        updateFormSubmitBtn();
        uploadErrorMessage.setInnerText("");

    }

    private void updateFormSubmitBtn() {
        formSubmitBtn.setEnabled((!urlTb.getText().isEmpty() || !fileBox.getFilename().isEmpty()) && !nameTb.getText().isEmpty());
    }
    public void setUploadUrl(String uploadUrl) {
        uploadFormPanel.setAction(uploadUrl);
    }
}
