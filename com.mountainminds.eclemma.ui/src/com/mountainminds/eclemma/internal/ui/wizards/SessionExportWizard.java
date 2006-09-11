/*******************************************************************************
 * Copyright (c) 2006 Mountainminds GmbH & Co. KG
 * This software is provided under the terms of the Eclipse Public License v1.0
 * See http://www.eclipse.org/legal/epl-v10.html.
 *
 * $Id: $
 ******************************************************************************/
package com.mountainminds.eclemma.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import com.mountainminds.eclemma.core.CoverageTools;
import com.mountainminds.eclemma.core.ICoverageSession;
import com.mountainminds.eclemma.core.ISessionExporter;
import com.mountainminds.eclemma.internal.ui.EclEmmaUIPlugin;
import com.mountainminds.eclemma.internal.ui.UIMessages;

/**
 * The export wizard for coverage sessions.
 * 
 * @author Marc R. Hoffmann
 * @version $Revision: $
 */
public class SessionExportWizard extends Wizard implements IExportWizard {

  private static final String SETTINGSID = "SessionExportWizard"; //$NON-NLS-1$

  private SessionExportPage1 page1;

  public SessionExportWizard() {
    IDialogSettings pluginsettings = EclEmmaUIPlugin.getInstance()
        .getDialogSettings();
    IDialogSettings wizardsettings = pluginsettings.getSection(SETTINGSID);
    if (wizardsettings == null) {
      wizardsettings = pluginsettings.addNewSection(SETTINGSID);
    }
    setDialogSettings(wizardsettings);
    setDefaultPageImageDescriptor(
        EclEmmaUIPlugin.getImageDescriptor(EclEmmaUIPlugin.WIZBAN_EXPORT_SESSION));
    setNeedsProgressMonitor(true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
   *      org.eclipse.jface.viewers.IStructuredSelection)
   */
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    // TODO Auto-generated method stub

  }

  public void addPages() {
    addPage(page1 = new SessionExportPage1());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  public boolean performFinish() {
    page1.saveWidgetValues();
    return createReport();

  }

  private boolean createReport() {
    ICoverageSession session = page1.getSelectedSession();
    final ISessionExporter exporter = CoverageTools.getExporter(session);
    exporter.setFormat(page1.getReportFormat());
    exporter.setDestination(page1.getDestination());
    IRunnableWithProgress op = new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor)
          throws InvocationTargetException, InterruptedException {
        try {
          exporter.export(monitor);
        } catch (Exception e) {
          throw new InvocationTargetException(e);
        }
      }
    };
    try {
      getContainer().run(true, true, op);
    } catch (InterruptedException e) {
      return false;
    } catch (InvocationTargetException ite) {
      Throwable ex = ite.getTargetException();
      EclEmmaUIPlugin.log(ex);
      String title = UIMessages.ExportReportPage1ErrorDialog_title;
      String msg = UIMessages.ExportReportPage1ErrorDialog_message;
      msg = NLS.bind(msg, session.getDescription());
      IStatus status;
      if (ex instanceof CoreException) {
        status = ((CoreException) ex).getStatus();
      } else {
        status = new Status(IStatus.ERROR, EclEmmaUIPlugin.ID, IStatus.ERROR,
            String.valueOf(ex.getMessage()), ex);
      }
      ErrorDialog.openError(getShell(), title, msg, status);
      return false;
    }
    return true;
  }

}