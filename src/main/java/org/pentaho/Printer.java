package org.pentaho;

import netscape.javascript.JSObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * User: nbaker
 * Date: 7/19/12
 */
public class Printer extends JApplet {
  private int width, height, originX, originY;
  private JButton btn;

  private static String os=System.getProperty("os.name").toUpperCase();
  private BufferedImage trackingImage;
  private Point locationOnScreen;

  public void setDimensions(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public void init() {

    width = 1500;
    height = 600;
    btn = new JButton("");
    getContentPane().add(btn);
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        capture();
      }
    });

    trackingImage = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
    trackingImage.setRGB(0, 0, 3, 3, new int[]{new Color(255, 174, 201).getRGB(), new Color(255, 127, 39).getRGB(), new Color(0, 0, 0).getRGB(), new Color(237, 28, 36).getRGB(), new Color(63, 72, 204).getRGB(), new Color(34, 177, 76).getRGB(), new Color(181, 230, 29).getRGB(), new Color(255, 255, 255).getRGB(), new Color(200, 191, 231).getRGB()}, 0, 3);

    JSObject window = JSObject.getWindow(this);
  }

  /**
   *
   * This method borrows a lot of code from The Dojo Foundation's DOHRobot.java Applet
   *
   */
  private void trackDownMacOrigin() {

    JSObject window = JSObject.getWindow(this);

    //Not working for some reason: window.eval("setRobotTrackerVisible(true)");


    Point p = getLocationOnScreen();
    if (os.indexOf("MAC") != -1) {
      // Work around stupid Apple OS X bug affecting Safari 5.1 and FF4.
      // Seems to have to do with the plugin they compile with rather than the jvm itself because Safari5.0 and FF3.6 still work.
      p = new Point();
      int screen = 0;
      int dohscreen = -1;
      int mindifference = Integer.MAX_VALUE;
      GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
      try {
        for (screen = 0; screen < screens.length; screen++) {
          // get origin of screen in Java virtual coordinates
          Rectangle bounds = screens[screen].getDefaultConfiguration().getBounds();
          // take picture
          DisplayMode mode = screens[screen].getDisplayMode();
          int width = mode.getWidth();
          int height = mode.getHeight();
          int twidth = trackingImage.getWidth();
          int theight = trackingImage.getHeight();
          Robot screenshooter = new Robot(screens[screen]);

          BufferedImage screenshot = screenshooter.createScreenCapture(new Rectangle(0, 0, width, height));
          // Ideally (in Windows) we would now slide trackingImage until we find an identical match inside screenshot.
          // Unfortunately because the Mac (what we are trying to fix) does terrible, awful things to graphics it displays,
          // we will need to find the "most similar" (smallest difference in pixels) square and click there.

          System.out.println("Scanning for image");
          int x = 0, y = 0;
          for (x = 0; x <= width - twidth; x++) {
            for (y = 0; y <= height - theight; y++) {
              int count = 0;
              int difference = 0;
              scanImage:
              for (int x2 = 0; x2 < twidth; x2++) {
                for (int y2 = 0; y2 < theight; y2++) {
                  int rgbdiff = Math.abs(screenshot.getRGB(x + x2, y + y2) - trackingImage.getRGB(x2, y2));
                  difference = difference + rgbdiff;
                  // short circuit mismatches
                  if (difference >= mindifference) {
                    break scanImage;
                  }
                }
              }
              if (difference < mindifference) {
                System.out.println("Found our image!");
                // convert device coordinates to virtual coordinates by adding screen's origin
                p.x = x + (int) bounds.getX();
                p.y = y + (int) bounds.getY();
                mindifference = difference;
                dohscreen = screen;
              }
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (p.x == 0 && p.y == 0) {
        // shouldn't happen...
        throw new RuntimeException("Robot not found on screen");
      }
      locationOnScreen = p;
    }

    //Not working for some reason: window.eval("setRobotTrackerVisible(false)");
  }

  public void print(int origX, int origY, int width, int height) {

    this.originX = origX;
    this.originY = origY;
    this.width = width;
    this.height = height;

    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          btn.grabFocus();
          btn.requestFocus();
          btn.doClick();
        }
      });
    } catch (InterruptedException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (InvocationTargetException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }


  public void capture() {

    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        try {

          trackDownMacOrigin();

          Point loc = locationOnScreen != null ? locationOnScreen : btn.getLocationOnScreen();

          System.out.println(loc);
          loc = new Point((int) loc.getX() + originX, (int) loc.getY() + originY);
          System.out.println("X: " + loc.getX() + " | Y: " + loc.getY() + " W: " + width + " | H: " + height);


          Rectangle areaToCapture = new Rectangle(loc, new Dimension(width, height));
          Robot robot = new Robot(btn.getGraphicsConfiguration().getDevice());
          areaToCapture.translate(-btn.getGraphicsConfiguration().getBounds().x,
              -btn.getGraphicsConfiguration().getBounds().y);


          final BufferedImage image = robot.createScreenCapture(areaToCapture);

          JFileChooser chooser = new JFileChooser();
          int retVal = chooser.showSaveDialog(Printer.this.getContentPane());
          if (retVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            String name = selectedFile.getName();
            if (name.endsWith("png") == false) {
              name += ".png";
            }
            selectedFile = new File(selectedFile.getParentFile(), name);
            ImageIO.write(image, "png", selectedFile);

            PrinterJob pj = PrinterJob.getPrinterJob();
            final PageFormat pf = pj.pageDialog(pj.defaultPage());
            pj.setPrintable(new Printable() {
              public int print(Graphics graphics, PageFormat pageFormat, int i) throws PrinterException {
                if (i > 0) {
                  return (NO_SUCH_PAGE);
                } else {

                  Graphics2D g2d = (Graphics2D) graphics;
                  graphics.translate((int) pf.getImageableX(), (int) pf.getImageableY());
                  AffineTransform at = new AffineTransform();
                  at.translate((int) pf.getImageableX(), (int) pf.getImageableY());

                  //We need to scale the image properly so that it fits on one page.
                  double xScale = pageFormat.getImageableWidth() / image.getWidth();
                  double yScale = pageFormat.getImageableHeight() / image.getHeight();
                  // Maintain the aspect ratio by taking the min of those 2 factors and using it to scale both dimensions.
                  double aspectScale = Math.min(xScale, yScale);

                  at.scale(aspectScale, aspectScale);
                  g2d.drawRenderedImage(image, at);
                  // Turn double buffering back on
                  return (PAGE_EXISTS);
                }
              }
            }, pf);
            try {
              pj.printDialog();
              pj.print();


            } catch (PrinterException exc) {
              System.out.println(exc);
            }

          }
        } catch (IOException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
      }
    });
  }


  public static void main(String[] args) throws IOException {

    BufferedImage trackingImage = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
    trackingImage.setRGB(0, 0, 3, 3, new int[]{new Color(255, 174, 201).getRGB(), new Color(255, 127, 39).getRGB(), new Color(0, 0, 0).getRGB(), new Color(237, 28, 36).getRGB(), new Color(63, 72, 204).getRGB(), new Color(34, 177, 76).getRGB(), new Color(181, 230, 29).getRGB(), new Color(255, 255, 255).getRGB(), new Color(200, 191, 231).getRGB()}, 0, 3);


    File selectedFile = new File(
        "/Users/nbaker/temp", "test.png");
    ImageIO.write(trackingImage, "png", selectedFile);

  }
}
