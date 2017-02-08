package app;

import java.awt.List;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Detector {
	@FXML
	private ImageView view;
	@FXML
	private ImageView snapshotView;
	@FXML
	private ImageView adjustedView;
	@FXML
	private Slider threshold;
	
	private ScheduledExecutorService timer;
	private Mat referenceFrame;
	private Mat checkFrame;
	private Mat snapshotFrame;
	
	private int maxCorners;
	private Point[] referencePoints;
	private Point[] checkPoints;
	
	public void startcamera()
	{
		VideoCapture cap = new VideoCapture();
		if(cap.open(0))
		{
			Runnable framegrabber = new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					referenceFrame = new Mat();
					//Mat dst = Mat.zeros(referenceFrame.size(), CvType.CV_32FC1);
					
					//cap.read(referenceFrame);
					cap.grab();
					cap.retrieve(referenceFrame);
					System.out.println("referenceframe channels: " + referenceFrame.channels());
					Imgproc.resize(referenceFrame, referenceFrame, new Size(200, 200));
					Imgproc.GaussianBlur(referenceFrame, referenceFrame, new Size(5, 5), 0.0);
					Mat greyFrame = new Mat();
					
					Imgproc.cvtColor(referenceFrame, greyFrame, Imgproc.COLOR_BGR2GRAY);
					
					maxCorners = (int) threshold.getValue();
					//frame = getCornerHarris(frame,(int) threshold.getValue());
					greyFrame = goodFeatures(greyFrame);
					System.out.println("Updating view");
					updateImageView(mat2Image(greyFrame));
				}
			};
			
			timer = Executors.newSingleThreadScheduledExecutor();
			timer.scheduleAtFixedRate(framegrabber, 0, 100, TimeUnit.MILLISECONDS);
		}
		
		
	}
	
	public void updateImageView(Image image)
	{
		//System.out.println("Updating");
		onFXThread(view.imageProperty(), image);
	}
	
	public void updateSnapshotView(Image image)
	{
		//System.out.println("Updating");
		onFXThread(snapshotView.imageProperty(), image);
	}
	
	public void updateAdjustedView(Image image)
	{
		//System.out.println("Updating");
		onFXThread(adjustedView.imageProperty(), image);
	}
	
	private <T> void onFXThread(ObjectProperty<T> property, T value) {
		Platform.runLater(() -> {
			//System.out.println("Setting property");
			property.set(value);
		});
	}
	
	private Image mat2Image(Mat frame) {
		if(frame.type() != CvType.CV_8U)
		{
			frame.convertTo(frame, CvType.CV_8U);
		}
		try
		{
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		}
		catch (Exception e)
		{
			System.err.println("Cannot convert the Mat obejct: " + e);
			return null;
		}
	}
	
	private BufferedImage matToBufferedImage(Mat original) {
		// init
		int type = 0;
		
		if(original.channels() == 1)
		{
			type = BufferedImage.TYPE_BYTE_GRAY;
		}
		else if(original.channels() == 3)
		{
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		
		BufferedImage image = new BufferedImage(original.width(), original.height(), type);
		WritableRaster raster = image.getRaster();
		DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
		
		byte[] data = dataBuffer.getData();
		
		original.get(0, 0, data);
		return image;
	}
	
	private Mat goodFeatures(Mat frame)
	{
		Mat copy = frame.clone();
		
		if(maxCorners < 1)
		{
			maxCorners = 1;
		}
		
		MatOfPoint corners = new MatOfPoint();
		double qualityLevel = 0.01;
		double mindistance = 10;
		
		System.out.println("Checking points");
		
		//Imgproc.goodFeaturesToTrack(frame, corners, maxCorners, qualityLevel, mindistance, new Mat(), blocksize, useHarris, k);
		Imgproc.goodFeaturesToTrack(frame, corners, maxCorners, qualityLevel, mindistance);
		
		System.out.println("Got points");
		
		Point[] points = corners.toArray();
		
		System.out.println("number of corners detected: " + corners.total());
		System.out.println("Number of rows: " + corners.rows() + ", cols: " + corners.cols());
		
		for(int i = 0; i < corners.total(); i++)
		{
			Imgproc.circle(copy, points[i], 4, new Scalar(0), 1, 8, 0);
		}
		
		return copy;
	}
	
	private Mat getCornerHarris(Mat frame, int thresh)
	{
		Mat dst = new Mat();
		Mat dst_norm = new Mat();
		Mat dst_norm_scaled = new Mat();
		
		dst = Mat.zeros(frame.size(), CvType.CV_32FC1);
		
		int blockSize = 3;
		int apertureSize = 3;
		double k = 0.04;
		
		Imgproc.cornerHarris(frame, dst, blockSize, apertureSize, k);
		//Imgproc.cornerEigenValsAndVecs(frame, dst, blockSize, apertureSize);
		
		Core.normalize(dst, dst_norm, 0, 255, Core.NORM_MINMAX);
		Core.convertScaleAbs(dst_norm, dst_norm_scaled);
		
		for(int j = 0; j < dst_norm.rows(); j++)
		{
			for(int i = 0; i < dst_norm.cols(); i++)
			{
				if(dst_norm.get(j, i)[0] > thresh)
				{
					Imgproc.circle(dst_norm_scaled, new Point(i, j), 5, new Scalar(0), 2, 8, 0);
				}
			}
		}
		
		return dst_norm_scaled;
	}
	
	public void takeSnapshot()
	{
		Mat dst = new Mat();

		Imgproc.cvtColor(referenceFrame, dst, Imgproc.COLOR_BGR2GRAY);
		
		snapshotFrame = dst.clone();
		
		MatOfPoint corners = new MatOfPoint();
		double qualityLevel = 0.01;
		double mindistance = 10;
		
		Imgproc.goodFeaturesToTrack(dst, corners, maxCorners, qualityLevel, mindistance);
		
		referencePoints = corners.toArray();
		
		for(int i = 0; i < corners.total(); i++)
		{
			Imgproc.circle(snapshotFrame, referencePoints[i], 4, new Scalar(0), 1, 8, 0);
		}
		
		updateSnapshotView(mat2Image(snapshotFrame));
	}
	
	public void compareSnapshot()
	{
		Mat dst = new Mat();

		Imgproc.cvtColor(referenceFrame, dst, Imgproc.COLOR_BGR2GRAY);
		
		checkFrame = dst.clone();
		
		MatOfPoint corners = new MatOfPoint();
		double qualityLevel = 0.01;
		double mindistance = 10;
		
		Imgproc.goodFeaturesToTrack(dst, corners, maxCorners, qualityLevel, mindistance);
		
		checkPoints = corners.toArray();
		
		MatOfPoint referenceCorners = new MatOfPoint(referencePoints);
		
		Mat ref = referenceCorners.clone();
		Mat check = corners.clone();
		
		Mat transform = Calib3d.findHomography(new MatOfPoint2f(referencePoints), new MatOfPoint2f(checkPoints));
		//Mat transform = Imgproc.getAffineTransform(new MatOfPoint2f(referencePoints), new MatOfPoint2f(checkPoints));
		//Mat transform = Imgproc.getPerspectiveTransform(ref, corners);
				
		Mat M  = transform.inv();
		//Mat M = transform.clone();
		
		Mat warped = new Mat();
		
		Imgproc.warpPerspective(checkFrame, warped, M, checkFrame.size());
		
		//Imgproc.warpAffine(checkFrame, warped, M, checkFrame.size());
		
		System.out.println(transform.dump());
		/*
		for(int i = 0; i < corners.total(); i++)
		{
			Imgproc.circle(checkFrame, checkPoints[i], 4, new Scalar(0), 1, 8, 0);
		}
		*/
		updateAdjustedView(mat2Image(warped));
	}
}
