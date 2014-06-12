package com.boxysystems.scriptmonkey.intellij.icons;

import javax.swing.*;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: shameed
 * Date: Oct 14, 2008
 * Time: 10:54:49 AM
 */
public class Icons {

  public static final Icon MONKEY_ICON = Icons.getIcon("/com/boxysystems/scriptmonkey/intellij/icons/generalIcon.gif");
  public static final Icon TOOLBAR_ICON = Icons.getIcon("/com/boxysystems/scriptmonkey/intellij/icons/toolbarIcon.gif");
  public static final Icon CLEAR_ICON = Icons.getIcon("/com/boxysystems/scriptmonkey/intellij/icons/reset.png");
  public static final Icon CONFIGURE_ICON = Icons.getIcon("/com/boxysystems/scriptmonkey/intellij/icons/settings.png");
  public static final Icon SUSPEND_ICON = Icons.getIcon("/com/boxysystems/scriptmonkey/intellij/icons/suspend.png");


  public static final Icon RERUN_ICON = Icons.getIcon("/com/boxysystems/scriptmonkey/intellij/icons/rerun.png");
  public static final Icon CLOSE_ICON =
          Icons.getIcon("/com/boxysystems/scriptmonkey/intellij/icons/cancel.png");
  public static final Icon HELP_ICON =
          Icons.getIcon("/com/boxysystems/scriptmonkey/intellij/icons/help.png");

  private static ImageIcon getIcon(String location) {
    final URL resource = Icons.class.getResource(location);
    return new ImageIcon(resource);
  }
}