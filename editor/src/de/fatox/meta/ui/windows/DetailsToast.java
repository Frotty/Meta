package de.fatox.meta.ui.windows;

import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.widget.LinkLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.toast.ToastTable;

/**
 * Used to display toast with short message and additional details available in separate dialog after pressing "Details" button. For example exception with stacktrace as details.
 * @author Kotcrab
 */
public class DetailsToast extends ToastTable {
	private final String text;
	private final String detailsDialogTitle;
	private final String details;

	protected VisTable linkLabelsTable;

	public DetailsToast (String text, Throwable cause) {
		this(text, "Exception Details", "");
	}

	public DetailsToast (String text, String details) {
		this(text, "Details", details);
	}

	public DetailsToast (String text, String detailsDialogTitle, String details) {
		this.text = text;
		this.detailsDialogTitle = detailsDialogTitle;
		this.details = details;

		add(text).expand().fill().row();

		linkLabelsTable = new VisTable(true);
		add(linkLabelsTable).spaceRight(12).right();
		addLabels();
	}

	protected void addLabels () {
		LinkLabel detailsLabel = new LinkLabel("Details");
		detailsLabel.setListener(url -> getStage().addActor(new Dialogs.DetailsDialog(text, detailsDialogTitle, details).fadeIn()));
		linkLabelsTable.add(detailsLabel);
	}
}