package com.luch.imagesplayer.core;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;

/**
 * this is a pictures viewer. That means u can use it to display some images,
 * and also play music in background
 * 
 * @author Jack#luch2046@gmail.com
 * @since 20140330
 */
public class App {

	JFrame frame;
	final int WIDTH = 800;
	final int HEIGHT = 600;
	// 菜单变量
	JMenuBar menubar;
	JMenu fileMenu;
	JMenu aboutMenu;
	JMenuItem openItem;
	JMenuItem exitItem;
	JMenuItem aboutItem;
	// 图片显示面板
	JScrollPane jsp;
	JPanel imagepanel;
	JLabel label;
	ImageIcon img;
	String title = "零五计算机-PictrueViewer";
	Image imge;
	String imageFilePath = "";
	File[] files;
	File currentFile = null;
	int fp = 0;// file pointer
	boolean isActionFlag = true;
	static Logger log = Logger.getLogger(App.class);
	boolean isPicPlayEnd = false;
	String mp3Path = "";
	AudioInputStream audioInputStream = null;
	SourceDataLine sourceDataLine = null;
	AudioFormat audioFormat = null;
	boolean isStop = true;// control the play thread
	boolean hasStop = true; // display the play thread status

	// fetch the SplashScreen.
	private SplashScreen splash = SplashScreen.getSplashScreen();
	private Rectangle splashBounds;
	private Graphics2D g;

	public App() {
		drawSplashScreen();
		initComponent();
	}
	public static void main(String[] args) {
		new App();
	}

	/**
	 * draw a splashscreen
	 * 
	 * @author Jack
	 */
	public void drawSplashScreen() {
		initSplash();
		final String[] stages = {"stage 1", "stage 2", "stage 3"};
		int stage = 0;
		for (int i = 0; i <= 100; i += 5) {
			String status = "Loading " + stages[stage] + "...";
			if (splash != null)
				updateSplash(status, i);
			try {
				Thread.sleep(100);
				if (i == 30)
					stage = 1;
				else if (i == 60)
					stage = 2;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (splash != null)
			splash.close();
	}

	/**
	 * update the splashscreen
	 * @author Jack 
	 */
	private void updateSplash(String status, int progress) {
		if(splash == null) {
			return;
		}
		if (g == null) {
			return;
		}
		//to draw a progress bar.
		drawSplash(g, status, progress);
		splash.update();		
	}

	/**
	 * initialize the SplashScreen. 
	 * @author Jack
	 */
	private void initSplash() {
		if(splash == null) {
			log.error(" warn:there is no splash got. ");
		}  else {
			 splashBounds = splash.getBounds();
			 g = splash.createGraphics();
			 if (g == null) {
				 log.error(" No create Graphics2D ");
			 } else {
			     g.setColor(Color.green);
			     g.drawRect(0, 0, splashBounds.width - 1, splashBounds.height - 1);
			 }
		}
		
	}
	
	/**
	 * to draw a progress bar. 
	 * @author Jack
	 */
	private void drawSplash(Graphics2D g, String status, int progress) {
		int barWidth = splashBounds.width*50/100;
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(1, 10, splashBounds.width - 2, 20);
        g.setPaintMode();
        g.setColor(Color.BLACK);
        g.drawString(status, 10, 20);
        g.setColor(Color.BLACK);
        g.drawRect(10, 25, barWidth + 2, 10);
        g.setColor(Color.YELLOW);
        int width = progress*barWidth/100;
        g.fillRect(11, 26, width + 1, 9);
        g.setColor(Color.WHITE);
        g.fillRect(11 + width + 1, 26, barWidth - width, 9);
	}
	
	
	/**
	 * initialize the components
	 */
	public void initComponent() {
		log.info("initComponent method");
		imageFilePath = System.getProperty("user.dir") + "\\image";
		String ppth = getAppPath(App.class);
		log.info(ppth);
		mp3Path = imageFilePath + "\\icanplay.mp3";
		log.info(mp3Path);
		frame = new JFrame();
		//set the width and height.
		frame.setSize(800, 600);
		int w = (Toolkit.getDefaultToolkit().getScreenSize().width - WIDTH) / 2;
		int h = (Toolkit.getDefaultToolkit().getScreenSize().height - HEIGHT) / 2;
		frame.setLocation(w, h);
		frame.setTitle(title);
		menubar = new JMenuBar();
		frame.setJMenuBar(menubar);
		fileMenu = new JMenu("File");
		aboutMenu = new JMenu("About");
		menubar.add(fileMenu);
		menubar.add(aboutMenu);
		
		exitItem = new JMenuItem("Exit");
		aboutItem = new JMenuItem("About Me");
		//fileMenu.add(openItem);
		fileMenu.add(exitItem);
		aboutMenu.add(aboutItem);
		exitItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				log.info("Exit the Application");
				System.exit(0);
			}
		});
		
		aboutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				javax.swing.JOptionPane.showMessageDialog(frame, "作者：Jack \nEmail： luch2046@163.com");		
			}
		});
		
		label = new JLabel();
		imagepanel = new JPanel();
		imagepanel.add(label);
		jsp = new JScrollPane(imagepanel);
		frame.add(jsp, BorderLayout.CENTER);
		imge = Toolkit.getDefaultToolkit().getImage(imageFilePath);
		File file = new File(imageFilePath);
		System.out.println(file);
		files = file.listFiles(new PicFilter());
		System.out.println(files);
				
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setResizable(false);//disable the maximize button.
		playPic();
		playMp3();
	}
	
	
	
	/**
	 * play picture
	 * @author Jack 
	 */
	public void playPic() {
		Thread play = new Thread(new PlayPicThread());
		System.out.println("Pic Thread " + play.getName());
		play.start();	
	}
	
	/**
	 * play mp3 
	 * @author Jack
	 */
	public void playMp3() {
		try {
			isStop = true;//停止播放线程
			while(!hasStop) {
				System.out.print(".");
				try {
					Thread.sleep(10);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			//取得文件输入流
			audioInputStream = AudioSystem.getAudioInputStream(new File(mp3Path));
			System.out.println(new File(mp3Path).toURI().toURL());
			audioFormat = audioInputStream.getFormat();
			//转换MP3文件编码
			if (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
				audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
						audioFormat.getSampleRate(), 16, audioFormat.getChannels(),  
                        audioFormat.getChannels() * 2, audioFormat.getSampleRate(), false);
				audioInputStream = AudioSystem.getAudioInputStream(audioFormat, audioInputStream);
			}
			//打开输出设备
			DataLine.Info info = new DataLine.Info(SourceDataLine.class,audioFormat);
			sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
			sourceDataLine.open(audioFormat, sourceDataLine.getBufferSize());  
			sourceDataLine.start();
			//创建独立线程进行播放
			isStop = false;
			Thread playThread = new Thread(new PlayMp3Thread());
			System.out.println("Mp3 Thread " + playThread.getName());
			playThread.start();
//            int bufferSize = (int) audioFormat.getSampleRate() * audioFormat.getFrameSize();
//            byte[] buffer = new byte[bufferSize];            
//            int bytesRead = 0;
//            while (bytesRead >= 0) {  
//                bytesRead = audioInputStream.read(buffer, 0, buffer.length);  
//                if (bytesRead >= 0) {
//                	sourceDataLine.write(buffer, 0, bytesRead);  
//                }
//            }
		} catch(Exception e) {
			log.error(" error ", e);
			e.printStackTrace();
		}
	}
	
	
	/**
	 * @author Jack
     * 2012-9-24
     * <br>
     * inner class
	 */
	class PicFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			if (name.endsWith("jpg") || name.endsWith("gif")||name.endsWith("png")) {
				return true;
			} else {
				return false;
			}
		}
		
	}
	/**
	 * get the Application resource path 
	 */
	@SuppressWarnings("rawtypes")
	public String getAppPath(Class clazz) {
		ClassLoader loader = clazz.getClassLoader();
		//get the class file's full name
		String clazzName = clazz.getName() + ".class";
		Package pack = clazz.getPackage();
		String path = "";
		if(pack != null) {
			String packName = pack.getName();
			clazzName = clazzName.substring(packName.length() + 1);
			if(packName.indexOf(".") < 0) {
				path = packName + "/";
			} else {
				int start = 0, end =0;
				end = packName.indexOf(".");
				while(end != -1) {
					path = path + packName.substring(start, end) + "/";
					start = end + 1;
					end = packName.indexOf(".", start);
				}
				path = path + packName.substring(start) + "/";
			}
		}
		//path + clazzName
		URL url = loader.getResource(path + clazzName);
		String realPath = url.getPath();
		int pos = realPath.indexOf("file:");
		if(pos > -1) {
			realPath = realPath.substring(pos + 5);
		}
		pos = realPath.indexOf(path + clazzName);
		realPath = realPath.substring(0, pos - 1);
		if(realPath.endsWith("!")) {
			realPath = realPath.substring(0, realPath.lastIndexOf("/"));
		}	
		return realPath;
	}
	
	/**
	 * play mp3 thread 
	 * @author Jack
	 */
	class PlayMp3Thread extends Thread {
		byte tempBuffer[] = new byte[320];
		
		public void run() {
			try {
				int cnt;
				hasStop = false;
				// 读取数据到缓存数据
				System.out.println("before while clause" + audioInputStream.read(tempBuffer, 0, tempBuffer.length));
				while((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
					if(isStop) {
						System.out.println("isStop:" + isStop);
						break;
					}
					if(cnt > 0) {
						//写入缓存数据
						sourceDataLine.write(tempBuffer, 0, cnt);
					}
				}
				System.out.println("after while clause");
				//Block 等待临时数据被输出为空
				sourceDataLine.drain();
				sourceDataLine.close();
				hasStop = true;
				if(audioInputStream.read(tempBuffer, 0, tempBuffer.length) == -1) {
					playMp3();
				}
				
				log.info("in the PlayMp3Thread");
			} catch (Exception e) {
				log.error(" error ", e);
				System.exit(1);
			}
			System.out.println("end all");
		}
	}
	
	/**
	 *  play picture thread
	 *  @author Jack 
	 */
	class PlayPicThread extends Thread {
		//override the run method.
		public void run() {
			isPicPlayEnd = false;
			System.out.println(files.length);
			for(int i = 0; i < files.length; i++) {
				currentFile = files[i];
				System.out.println(currentFile);
				isActionFlag = true;
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e1) {
					log.error(" error ", e1);
				}
				setImage(currentFile, isActionFlag);
				if(i == files.length -1) {
					isPicPlayEnd = true;
				}
				int count = i + 1;
				log.info("count" + count);
				System.out.println("picture: " + count + "/" + files.length);				
			}
			//loop play pictures.
			if(isPicPlayEnd) {
				playPic();
				log.info("in the PlayPicThread/loop");
				System.out.println("play again");
			}		
		}
	}
	
	/**
	 * to set the image size. (set pictures to the same size)
	 * @param file
	 * @param isActive 
	 * 
	 */
	public void setImage(File file, boolean isActive) {
		img = new ImageIcon(file.getPath().toString());
		System.out.println("pic file path: " + file.getPath());
		int cw;
		int ch;
		int iw = img.getIconWidth();
		int ih = img.getIconHeight();
		if(isActive) {
			cw = jsp.getWidth();
			ch = jsp.getHeight();
			if (iw > cw || ih > ch) {
				if (cw / ch > iw / ih) {
					iw = iw * (ch - 50) / ih;
					ih = ch - 50;
					img.setImage(setFixed(file, iw, ih));
				} else {
					ih = (cw - 50) * ih / iw;
					iw = cw - 50;
					img.setImage(setFixed(file, iw, ih));
				}
			}
			imagepanel.setLayout(null);
			label.setBounds((cw - iw) / 2, (ch - ih) / 2, iw, ih);
		} else {
			cw = imagepanel.getWidth();
			ch = imagepanel.getHeight();
			imagepanel.setLayout(new java.awt.FlowLayout());
		}
		label.setIcon(img);
	}
	
	/**
	 * change the picture size. 
	 * @param file
	 * @param width
	 * @param height
	 */
	public Image setFixed(File file, int width, int height) {
		BufferedImage bi = null;
		
		try {
			bi = javax.imageio.ImageIO.read(file);
		} catch (IOException ioe) {
			log.error(" error ", ioe);			
			ioe.printStackTrace();
		}
		
		return bi.getScaledInstance(width, height, Image.SCALE_SMOOTH);
	}
}
