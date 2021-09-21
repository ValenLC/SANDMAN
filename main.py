'''
Created on 10 Feb 2020

@author: Anders Appel
'''

import sys
import csv
import os, glob
import json
import pyqtgraph
import pandas as pd

from lib.widgets import MatPlot, List, CustomSlider, ExcelTable
# import MatPlot

from datetime import datetime
from enum import Enum
from PyQt5.QtCore import QTimer, Qt
from PyQt5.QtGui import QColor, QFont, QIcon, QPixmap
from PyQt5.QtWidgets import (QApplication, QWidget, QPushButton, QLabel, QMenu,
				QMainWindow, QDesktopWidget, QAction, QGridLayout, QSizePolicy,
				QFileDialog, QComboBox, QSlider, QCheckBox)



		
class FeedbackMode(Enum):
	MANUAL = 1
	AUTOMATIC_HOUR = 2
	AUTOMATIC_DAY = 3
	AUTOMATIC_WEEK = 4

class Window(QMainWindow):
	
	INITIAL_TIMERS_SPEED = 1024 # For the multiplication and division of speed by 2
	internalTimerSpeed = INITIAL_TIMERS_SPEED/4 # Update time in ms
	externalTimerSpeed = INITIAL_TIMERS_SPEED # Speed time of the application in ms

	def __init__(self):
		super().__init__()

		# Define system variables
		
		self.source_file_name = '20Sensors_7profiles.xlsx - Data1_8mois.csv5%&15%_bruit.csv' # Input CSV file
		#self.source_file_name = 'data4-double.csv'
        
        
		self.paused = True
		self.started = False
		self.filtering = False
		self.learning = False
		self.line_counter = 0
		self.data_points = 24
		self.anomaly_degrees = [0.0]*24
		self.anomaly_degrees2 = [0.0]*24
		self.thresholds = [1000.0]*24
		self.current_data = [[0]*(self.data_points+2)]
		self.current_profiles = [[0]*(self.data_points+1) for i in range(20)]
		self.current_weights = [[0]*(self.data_points+1) for i in range(20)]
		self.current_data2 = [[0]*self.data_points]
		self.current_profiles2 = [[0]*self.data_points for i in range(20)]
		self.current_weights2 = [[0]*self.data_points for i in range(20)]
		self.threshold = 100
		self.selected_sensors = [1] # Indexes of the sensors to plot
		self.selected_column = -1
		self.previous_feedback_timestamp = 'Null'
		self.colors = ['r', 'g', 'b', 'c', 'm', 'y', 'k']
		self.nbTruePositives = 0
		self.nbTrueNegatives = 0
		self.nbFalsePositives = 0
		self.nbFalseNegatives = 0
		self.anomaly_degrees_points = []
		self.premierFeedback = 'null'
		self.lastFeedback = 'null'
		self.last_timestamp = 'null'

		self.feedbackMode = FeedbackMode.MANUAL
		
		# Define font for labels
		self.label_font = QFont("Arial", 12, QFont.Bold)

		# Path to save file for record of feedback
		self.save_path = os.path.join(os.path.curdir, 'Data')
		self.save_file = 'Feedback_Record.csv'

		with open(os.path.join(self.save_path, self.save_file), 'w') as save_file:
			writer = csv.writer(save_file)
			writer.writerow(["Time Stamp", "Feedback", "Sandman Output (isAnom)"])

		# Sandman Output Directory Path
		self.output_path = os.path.join(os.path.curdir, 'Anomasly', 'Output')
		self.output_list = dict([(f, None) for f in os.listdir(self.output_path)])

		# Sandman Input Directory Path
		self.input_path = os.path.join(os.path.curdir, 'Anomasly', 'Input')
		self.input_file_start = "start.json"
		self.input_file_play = "play.json"
		self.input_file_feedback = "feedback.json"
		self.input_file_timer = "timer.json"

		# Remove any leftover files in the input and output directories
		self.clearDirectories()

		self.initialiseUI()

		# Load a file
		self.data_file = os.path.join(os.path.curdir, 'Anomasly', 'Data', self.source_file_name)
		self.loadFile(self.data_file)
		self.default_timestamp = self.data[0][0]
		self.selected_timestamp = self.default_timestamp

		self.timer = QTimer()
		self.timer.timeout.connect(self.tick)
		self.timer.start(Window.internalTimerSpeed)

	def initialiseUI(self):
		# Label for raw data table
		self.table_label = QLabel()
		self.table_label.setText("Raw Sensor Data")
		self.table_label.setAlignment(Qt.AlignCenter)
		self.table_label.setFont(self.label_font)

		# Table to display raw data
		self.table = ExcelTable('Data/example_results.csv')
		self.table.itemSelectionChanged.connect(self.tableSelection)
		self.table.setMinimumSize(350, 500)

		# Array with timestamps and anomaly values
		csvData = csv.reader(open('Anomasly/Data/' + self.source_file_name, 'r'), delimiter=",")
		self.columnTimestamps, columnAnomalies = [], []
		for row in csvData:
			self.columnTimestamps.append(row[0])
			if row[21] == 'A':
				columnAnomalies.append(True)
			else:
				columnAnomalies.append(False)
		self.arrayData = [self.columnTimestamps, columnAnomalies]

		# Label for selection
		self.table_selection_label = QLabel()
		self.table_selection_label.setText("No Columns Selected")
		self.table_selection_label.setAlignment(Qt.AlignCenter)

		# Button to restart simulation
		self.restart_button = QPushButton('Restart Simulation', self)
		self.restart_button.clicked.connect(self.restartButton)
		self.restart_button.setToolTip('Restart the simulation from the selected timestamp (defaults to first timestamp if none selected)')

		# Button for adding sensors
		self.toggle_sensor_button = QPushButton('Toggle Visibility', self)
		self.toggle_sensor_button.clicked.connect(self.toggleSensor)
		self.toggle_sensor_button.setToolTip('Add or remove selected sensor from the plot (only select one sensor column at a time)')

		# Figure for the graph displaying the sensors values
		#self.plotter_sensors = MatPlot(self, width=5, height=2)
		#self.plotter_sensors.setMinimumSize(500, 250)
		pyqtgraph.setConfigOption('background', 'w')
		self.plotter_sensors = pyqtgraph.PlotWidget()
		self.plotter_sensors.hideAxis('bottom')
		self.plotter_sensors.setMinimumSize(500, 250)
		self.plotter_sensors.addLegend()
		pyqtgraph.setConfigOption('background', 'w')

		# Figure for the graph displaying the anomaly degrees
		pyqtgraph.setConfigOption('background', 'w')
		self.plotter_degrees = pyqtgraph.PlotWidget()
		self.plotter_degrees.hideAxis('bottom')
		self.plotter_degrees.setMinimumSize(500, 250)
		pyqtgraph.setConfigOption('background', 'w')
		self.penDegree = pyqtgraph.mkPen(color=(255, 0, 0))
		self.penDegree = pyqtgraph.mkPen(color=(0, 0, 255))
		self.plotter_degrees.addLegend()
		self.plot_thresholds = self.plotter_degrees.plot(x=[0], y=[0])

		# Button for playing and pausing the simulation
		self.play_button = QPushButton('Play', self)
		self.play_button.clicked.connect(self.playButton)
		self.play_button.setToolTip('Starts and stops the simulation')
		
		# Slider to speed or slow the simulation
		self.speed_slider = QSlider(Qt.Horizontal, self)
		self.speed_slider.setMinimum(1)
		self.speed_slider.setMaximum(5)
		self.speed_slider.setValue(1)
		self.speed_slider.valueChanged.connect(self.speedChange)
		self.speed_slider.setToolTip('Change the speed of the simulation')
		
		# Label displaying the speed of execuction
		self.speed_label = QLabel('Speed : [×' + str(Window.INITIAL_TIMERS_SPEED/Window.externalTimerSpeed) + ']')

		# Label for list
		self.list_label = QLabel()
		self.list_label.setText("Sandman Outputs")
		self.list_label.setAlignment(Qt.AlignCenter)
		self.list_label.setFont(self.label_font)

		# List of past decisions to evaluate
		self.decision_list = List()
		self.decision_list.setMinimumSize(200, 500)	
		self.decision_list_copy = List() # Original list
		self.filtered_decision_list = List() # List only showing ALERT


		# Button to export the report
		self.export_report = QPushButton('Export report', self)
		self.export_report.clicked.connect(self.exportReport)
		self.export_report.setToolTip('Exports a .txt file with the report of the stats for the session')

		# Button to filter the list
		self.filter_button = QPushButton('Filter List', self)
		self.filter_button.clicked.connect(self.filterButton)
		self.filter_button.setToolTip('Filters list to only show ALERT outputs')

		# Button to mark items correct
		self.correct_button = QPushButton('Correct', self)
		self.correct_button.clicked.connect(self.correctButton)
		self.correct_button.setToolTip('Marks items as correct for sandman feedback')

		# Button to mark items incorrect
		self.incorrect_button = QPushButton('Incorrect', self)
		self.incorrect_button.clicked.connect(self.incorrectButton)
		self.incorrect_button.setToolTip('Marks items as incorrect for sandman feedback')

		# Button to validate previous selections
		self.validate_button = QPushButton('Validate', self)
		self.validate_button.clicked.connect(self.validateButton)
		self.validate_button.setToolTip('Validates currently selected feedback and sends it to sandman')
		
		# Checkbox to enable/disable the display of sensors values
		self.displayValueCheckBox = QCheckBox("Values")
		self.displayValueCheckBox.setChecked(True)
		
		# Checkbox to enable/disable the display of sensors profiles
		self.displayProfileCheckBox = QCheckBox("Profiles")
		self.displayProfileCheckBox.setChecked(False)
		
		# Checkbox to enable/disable the display of sensors weights
		self.displayWeightCheckBox = QCheckBox("Weights")
		self.displayWeightCheckBox.setChecked(False)
		
		# Logos of the app
		self.label_logos = QLabel(self)
		pixmap_logos = QPixmap('Assets/Images/logos-interface.png')
		self.label_logos.setPixmap(pixmap_logos)
		
		# Combobox for the feedback mode
		self.feedback_mode_combobox = QComboBox()
		self.feedback_mode_combobox.addItems(["Manual", "Automatic (hour)", "Automatic (day)", "Automatic (week)"])
		self.feedback_mode_combobox.currentIndexChanged.connect(self.changeFeedbackMode)
		self.feedback_mode_combobox.setToolTip('Change the feedback mode')
		
		# Set layout
		main_widget = QWidget()
		grid = QGridLayout()
		
		grid.addWidget(self.label_logos, 0, 0)
		
		grid.addWidget(self.table_label, 1, 0, 1, 2)
		grid.addWidget(self.table, 2, 0, 3, 2)
		grid.addWidget(self.table_selection_label, 5, 0, 1, 2)
		grid.addWidget(self.restart_button, 6, 0)
		grid.addWidget(self.toggle_sensor_button, 6, 1)

		grid.addWidget(self.plotter_sensors, 2, 3, 1, 5)
		grid.addWidget(self.displayValueCheckBox, 1, 3)
		grid.addWidget(self.displayProfileCheckBox, 1, 4)
		grid.addWidget(self.displayWeightCheckBox, 1, 5)
		grid.addWidget(self.plotter_degrees, 3, 3, 1, 5)
		grid.addWidget(self.play_button, 6, 3)
		grid.addWidget(self.speed_label, 5, 5, 1, 3)
		grid.addWidget(self.speed_slider, 6, 4, 1, 3)
		grid.addWidget(self.feedback_mode_combobox, 5, 7)
		grid.addWidget(self.export_report, 6, 7)

		grid.addWidget(self.list_label, 1, 8, 1, 2)
		grid.addWidget(self.decision_list, 2, 8, 2, 2)
		grid.addWidget(self.correct_button, 5, 8)
		grid.addWidget(self.incorrect_button, 5, 9)
		grid.addWidget(self.filter_button, 6, 9)
		grid.addWidget(self.validate_button, 6, 8, 1, 1)

		main_widget.setLayout(grid)
		self.setCentralWidget(main_widget)

		self.initialiseMenuBar()

		# Initialise status bar
		self.statusBar().showMessage("")

		self.setWindowTitle('Sandman Graphical Interface')
		self.resize(1100, 500)
		self.setWindowIcon(QIcon("Assets/Images/transparent-icon.png"))
		self.show()

	""" Creates the menu bar at the top left """
	def initialiseMenuBar(self):
		# Create menu bar
		self.menubar = self.menuBar()
			
		# Create file tab
		#self.file_menu = self.menubar.addMenu('&File')

		# Submenus
		#import_menu = QMenu('Import', self)
		#import_csv = QAction('CSV', self)
		#import_csv.triggered.connect(self.importDialogCSV)
		#import_menu.addAction(import_csv)
		#self.file_menu.addMenu(import_menu)

		#select_save = QAction('Select Save Location', self)
		#select_save.triggered.connect(self.selectSaveDialog)
		#self.file_menu.addAction(select_save)

	""" Executes when play/pause button is pressed, starts and stops sandman """
	def playButton(self):
		if self.paused and not self.started:
			self.started = True
			self.timer.start()
			self.paused = False
			self.play_button.setText("Pause")

			# Write the file to start sandman
			self.writeStartFile(self.selected_timestamp)
		elif self.paused and self.started:
			self.timer.start()
			self.paused = False
			self.play_button.setText("Pause")

			# Write the file to tell sandman to run
			self.writePlayFile(running=True)
		else:
			self.timer.stop()
			self.paused = True
			self.play_button.setText("Play")

			# Write file to tell sandman to stop running
			self.writePlayFile(running=False)
			
			
	""" Executes when the speed slider is moved, speeds or slows the simulation """
	def speedChange(self):
		# from 2^5 to 2^15
		Window.externalTimerSpeed = pow(2,(11-self.speed_slider.value()))
		self.speedChangeInternal()
		#Window.internalTimerSpeed = pow(2,(15-self.speed_slider.value()))
		self.speedChangeInternal()
		self.timer.timeout.connect(self.tick)
		self.timer.start(Window.internalTimerSpeed)

		# Write file to tell sandman to update the timer
		self.writeTimerFile(Window.externalTimerSpeed)
		self.speed_label.setText('Speed : [×' + str(Window.INITIAL_TIMERS_SPEED/Window.externalTimerSpeed) + ']')

	""" Changes the internal speed of the UI """
	def speedChangeInternal(self):
		# from 2^5 to 2^15
		Window.internalTimerSpeed = pow(2,(8-self.speed_slider.value()))
		self.timer.timeout.connect(self.tick)
		self.timer.start(Window.internalTimerSpeed)


	""" Executes when selection is changed on table """
	def tableSelection(self):
		columns = [] # List of columns selected
		rows = [] # List of rows selected
		self.selected_timestamp = self.default_timestamp

		# Iterate through selected items
		for i in range(len(self.table.selectedItems())):
			col = self.table.column(self.table.selectedItems()[i])
			row = self.table.row(self.table.selectedItems()[i])

			if col not in columns:
				columns.append(col)

			if row not in rows:
				rows.append(row)

		# Update the text on the label to reflect what is selected
		if len(columns) == 1:
			# Only one column is selected
			self.selected_column = columns[0]

			if columns[0] != 0:
				# Sensor column selected
				label_text = 'Column Selected: Sensor ' + str(columns[0])
				self.table_selection_label.setText(label_text)
			elif columns[0] == 0 and len(rows) == 1:
				# One row of timestamp column selected
				self.selected_timestamp = self.table.selectedItems()[0].text()
				label_text = 'Timestamp Selected: ' + self.selected_timestamp
				self.table_selection_label.setText(label_text)
			elif columns[0] == 0 and len(rows) > 1:
				# Multiple rows of timestamp column selected
				self.table_selection_label.setText('ERROR: Multiple Timestamps Selected')
		elif len(columns) == 0:
			# No columns selected
			self.table_selection_label.setText('No Columns Selected')
		else:
			# Multiple columns selected
			self.table_selection_label.setText('ERROR: Multiple Columns Selected')

	""" Restarts simulation from selected timestamp """
	def restartButton(self):
		# Clear input/output directories
		self.clearDirectories()

		# Clear current data and display lists
		for i in range(len(self.current_data)):
			self.current_data[i] = [0]*self.data_points
		self.anomaly_degrees.clear()
		self.thresholds.clear()
		self.decision_list.clear()
		self.decision_list_copy.clear()
		self.filtered_decision_list.clear()

		for i in range(len(self.data)):
			if self.selected_timestamp in self.data[i]:
				self.line_counter = i
				break

		self.writeStartFile(self.selected_timestamp)

	""" Adds or removes selected sensor to plot """
	def toggleSensor(self):
		if self.selected_column > 0 and self.selected_column not in self.selected_sensors:
			self.addSensor(self.selected_column)
		elif self.selected_column > 0 and self.selected_column in self.selected_sensors:
			self.removeSensor(self.selected_column)

	""" Adds the sensor to the data array """
	def addSensor(self, sensor):
		if(self.isFloat(self.data[self.line_counter][sensor])):
			# Add a new set of data to the current data
			self.selected_sensors.append(sensor)
			self.current_data.append([0]*self.data_points)
			new_length = len(self.selected_sensors)

			# Add the new sensor data
			for i in range(self.data_points):
				ind = self.line_counter - self.data_points + i
				if ind >= 0:
					self.current_data[new_length-1][i] = float(self.data[ind][sensor])
				else:
					self.current_data[new_length-1][i] = 0

			self.tick(False)
		else:
			print("Value Error: Data must be float")

	""" Removes sensor from data array """
	def removeSensor(self, sensor):
		index = self.selected_sensors.index(sensor)
		del self.selected_sensors[index]
		del self.current_data[index]

	""" Executes when an item is selected from the drop down menu """
	def dropDownSelect(self, index):
		if(self.isFloat(self.data[self.line_counter][index])):
			self.sensor = index

			# Refresh the current data with the new index
			for i in range(self.data_points):
				ind = self.line_counter - self.data_points + i
				if ind >= 0:
					self.current_data[i] = float(self.data[ind][self.sensor])
				else:
					self.current_data[i] = 0

			self.tick(False)
		else:
			print("Value Error: Data must be float")
			self.drop_down.setCurrentIndex(self.sensor)

	""" Check if value can be converted to float """
	def isFloat(self, val):
		try:
			float(val)
			return True
		except ValueError:
			return False
			

	""" Exports a report with stats of the session"""
	def exportReport(self):
		now = datetime.now()
		file_name = str(now.strftime("Reports\\Rapport_du_%d-%m-%Y_a_%H-%M-%S.txt"))
		file_path = os.path.join(os.getcwd(), file_name)
		print("date and time =", file_name)	
		file = open(file_path,"w+")
		file.write("Report on file '" + self.source_file_name + "':")
		file.write("\n[from " + str(self.premierFeedback) + " to " + str(self.lastFeedback) + "]")
		file.write("\n----------------------------------")
		file.write("\n• True Positives: " + str(self.nbTruePositives))
		file.write("\n• True Negatives: " + str(self.nbTrueNegatives))
		file.write("\n• False Positives: " + str(self.nbFalsePositives))
		file.write("\n• False Negatives: " + str(self.nbFalseNegatives))
		file.write("\n----------------------------------")
		file.write("\nTotal execution time: " + str(self.totalExecutionTime) + "ms")
		self.exportCSV()
	
	# TODO REMOVE TEST
	def exportCSV(self):
		now = datetime.now()
		file_name1 = str(now.strftime("Reports\\Poids%d-%m-%Y_a_%H-%M-%S.txt"))
		file_name2 = str(now.strftime("Reports\\Valeurs%d-%m-%Y_a_%H-%M-%S.txt"))
		file_name3 = str(now.strftime("Reports\\Profils%d-%m-%Y_a_%H-%M-%S.txt"))

		file_path1 = os.path.join(os.getcwd(), file_name1)
		file_path2 = os.path.join(os.getcwd(), file_name2)
		file_path3 = os.path.join(os.getcwd(), file_name3)

		file1 = open(file_path1,"w+")
		file2 = open(file_path2,"w+")
		file3 = open(file_path3,"w+")
################################################################
		df_weights = pd.DataFrame(self.current_weights2)
		df_weights = df_weights.transpose()
		df_values = pd.DataFrame(self.current_data2)   
		df_values = df_values.transpose()
		df_profiles = pd.DataFrame(self.current_profiles2)
		df_profiles = df_profiles.transpose()
		df_DA = pd.DataFrame(self.anomaly_degrees2)

		print(df_values)
		print(df_weights)
		print(df_profiles)
        

		nb_sensors = len(df_weights.columns)
		lst_column_df_weights=[]
		for i in range(1, nb_sensors+1):
			lst_column_df_weights.append("s"+str(i)+"_weights")
		df_weights.columns = lst_column_df_weights
              
		""" df_values = pd.DataFrame(self.current_data2)

		lst_column_df_values=[]
		for i in range(1, nb_sensors+1):
			lst_column_df_values.append("s"+str(i)+"_values")
		print(len(df_values.columns))
		print(lst_column_df_values)
		df_values.columns = lst_column_df_values"""
                

		lst_column_df_profiles=[]
		for i in range(1, nb_sensors+1):
			lst_column_df_profiles.append("s"+str(i)+"_profiles")
		df_profiles.columns = lst_column_df_profiles
        
		df = pd.concat([df_profiles, df_weights], axis=1)
		df=df.sort_index(axis=1)
		csv_name = str(now.strftime("Reports\\report%d-%m-%Y_a_%H-%M-%S.csv"))
		df.to_csv (csv_name, index = False, header=True)
        
		csv_DA = str(now.strftime("Reports\\DA_du_%d-%m-%Y_a_%H-%M-%S.csv"))
		df_DA.to_csv (csv_DA, index = False, header=True)
################################################################   
		file1.write(str(self.current_weights2) + ",")
		file2.write(str(self.current_data2) + ",")
		file3.write(str(self.current_profiles2) + ",")
		

	""" Executes when the 'filter list' button is pressed """
	def filterButton(self):
		if self.filtering:
			self.filtering = False
			self.filter_button.setText('Filter List')
			self.decision_list.copyList(self.decision_list_copy)
		else:
			self.filtering = True
			self.filter_button.setText('Stop Filtering')
			self.decision_list.copyList(self.filtered_decision_list)
	
	""" Searches in the CSV data file if the line corresponding to the timestamp is listed as an anomaly """
	def getAnomalyValueFromTimestamp(self, timestamp):
		index = self.arrayData[0].index(timestamp)
		return self.arrayData[1][index]

	""" Slimutates a selection of all the items on th decision list and a click on the 'Validate' button """
	def validateAllValues(self):
		self.decision_list.selectAll()
		#for item in self.decision_list.selectedItems():
		#	item.setBackground(QColor("green"))
		self.validateButton()
		self.lastFeedback =	self.decision_list.item(0).text()[0:16]


	""" Executes at each iteration in direct feedback mode """
	def validateDirectMode(self):
		if self.started:
			feedback_array = []
			if self.lastFeedback != 'null':
				nbTimestampsToValidate = self.columnTimestamps.index(self.last_timestamp) - self.columnTimestamps.index(self.lastFeedback)
			else:
				nbTimestampsToValidate = 0
				self.validateAllValues()
			for indexTimestamp in range(nbTimestampsToValidate):
				item = self.decision_list.item(nbTimestampsToValidate - indexTimestamp-1)
				feedback = {}
				# Split the text of the list into timestamp and output
				timestamp = item.text()[0:16]
			
				if self.premierFeedback == 'null':
					self.premierFeedback = timestamp
				self.lastFeedback = timestamp
			
				if timestamp == '2016-01-07T23:00':
					self.validateAllValues()
					return
				if (timestamp[8] != '0' and timestamp != self.previous_feedback_timestamp) or timestamp[6] == '2' or timestamp[6] == '3' or timestamp[6] == '4'  or timestamp[6] == '5' or timestamp[6] == '1':
				# or timestamp[5] == '0'
				#if timestamp != self.previous_feedback_timestamp:
					self.previous_feedback_timestamp = timestamp
					output = item.text()[19:]
					isCurrentAnom = output
					isCurrentDetectedAsAnomaly = (output == 'ALERT') # 'REGULAR' -> not an anomaly
					isCurrentReallyAnomaly = self.getAnomalyValueFromTimestamp(timestamp)
					print('----------------------')
					print('Time: ' + timestamp)
					print('System: ' + str(isCurrentDetectedAsAnomaly))
					print('File: ' + str(isCurrentReallyAnomaly))
					if isCurrentDetectedAsAnomaly == isCurrentReallyAnomaly:
						# The item is correct
						feedback["timeStamp"] = timestamp
						feedback["expertFeedback"] = "VALID"
						feedback["isAnom"] = self.isAnom("VALID", output)
						feedback_array.append(feedback)
						item.setBackground(QColor("lightGreen"))
						print('Feedback -> CORRECT')
						if output == 'ALERT':
							self.nbTruePositives += 1
						else:
							self.nbTrueNegatives += 1
							print("VALIDE -------- : " + item.text())
					else:
						# The item is incorrect
						feedback["timeStamp"] = timestamp
						feedback["expertFeedback"] = "INCORRECT"
						feedback["isAnom"] = self.isAnom("INCORRECT", output)
						feedback_array.append(feedback)
						item.setBackground(QColor("darkRed"))
						print('Feedback -> INCORRECT')
						if output == "ALERT":
							self.nbFalsePositives += 1
						else:
							self.nbFalseNegatives += 1

					# Write the feedback array to the correct input file
					self.writeFeedbackFile(feedback_array)

					# Save the expert's feedback to the save file
					self.writeSaveFile(feedback_array)
					print("TP : "+str(self.nbTruePositives))
					#print(str(self.nbTruePositives))
					print("TN : "+str(self.nbTrueNegatives))
					#print(str(self.nbTrueNegatives))
					print("FP : "+str(self.nbFalsePositives))
					#print(str(self.nbFalsePositives))
					print("FN : "+str(self.nbFalseNegatives))
					#print(str(self.nbFalseNegatives))
						
				if timestamp == '2016-01-31T23:00' or timestamp == '2016-03-02T23:00' or timestamp == '2016-04-02T23:00' or timestamp == '2016-05-02T23:00':
					self.exportReport()

	""" Executes when 'validate' button is pressed to remove flagged items """
	def validateButton(self):
		feedback_array = []

		for indexList in range(self.decision_list.count()):
			# Get the current item
			item = self.decision_list.item(indexList)
			feedback = {}

			# Split the text of the list into timestamp and output
			timestamp = item.text()[0:16]
			output = item.text()[19:]
			
			if self.premierFeedback == 'null':
				self.premierFeedback = timestamp
			self.lastFeedback = timestamp

			# Determine which decision was made by the expert
			color = item.background().color()
			if color == QColor("white"):
				# The item has been left blank, and so will be validated
				feedback["timeStamp"] = timestamp
				feedback["expertFeedback"] = "VALID"
				feedback["isAnom"] = self.isAnom("VALID", output)
				feedback_array.append(feedback)
				item.setBackground(QColor("lightGreen"))
				if output == 'ALERT':
					self.nbTruePositives += 1
				else:
					self.nbTrueNegatives += 1
					print("VALIDE -------- : " + item.text())
			elif color == QColor("green"):
				# The item has been marked as correct
				feedback["timeStamp"] = timestamp
				feedback["expertFeedback"] = "VALID"
				feedback["isAnom"] = self.isAnom("VALID", output)
				feedback_array.append(feedback)
				item.setBackground(QColor("lightGreen"))
				if output == 'ALERT':
					self.nbTruePositives += 1
				else:
					self.nbTrueNegatives += 1
					print("VALIDE -------- : " + item.text())

			elif color == QColor("red"):
				# The item has been marked as incorrect
				feedback["timeStamp"] = timestamp
				feedback["expertFeedback"] = "INCORRECT"
				feedback["isAnom"] = self.isAnom("INCORRECT", output)
				feedback_array.append(feedback)
				item.setBackground(QColor("darkRed"))
				if output == "ALERT":
					self.nbFalsePositives += 1
				else:
					self.nbFalseNegatives += 1

		# Write the feedback array to the correct input file
		self.writeFeedbackFile(feedback_array)

		# Save the expert's feedback to the save file
		self.writeSaveFile(feedback_array)


	""" Executes when 'incorrect' button is pressed to flag items red """
	def incorrectButton(self):
		for item in self.decision_list.selectedItems():
			# Change item colour
			item.setBackground(QColor("red"))

			# Deselect items
			item.setSelected(False)

	""" Executes when 'correct' button is pressed to flag items green """
	def correctButton(self):
		for item in self.decision_list.selectedItems():
			# Change item colour
			item.setBackground(QColor("green"))

			# Deselect items
			item.setSelected(False)

	""" Update the current profiles array """
	def updateProfilesArrays(self, sensorsProfilesString):
		# removes '[' and ']' from the data
		sensorsProfiles = sensorsProfilesString[1:-1].split(',')
		if sensorsProfiles[0] != 'null':
			for sensor in range(len(sensorsProfiles)):
				self.current_profiles[sensor].append(float(sensorsProfiles[sensor]))
				self.current_profiles2[sensor].append(float(sensorsProfiles[sensor]))
				if(len(self.current_profiles[sensor]) > 25):
					del(self.current_profiles[sensor][0])
				
	""" Update the current weights array """
	def updateWeightsArrays(self, sensorsWeightsString):
		# removes '[' and ']' from the data
		sensorsWeights = sensorsWeightsString[1:-1].split(',')
		if sensorsWeights[0] != 'null':
			for sensor in range(len(sensorsWeights)):
				self.current_weights[sensor].append(float(sensorsWeights[sensor]))
				self.current_weights2[sensor].append(float(sensorsWeights[sensor]))
				if(len(self.current_weights[sensor]) > 25):
					del(self.current_weights[sensor][0])

	""" Plot data about the chosen sensors according to the checkboxes selected (values, nominal values and weights) """
	def plotSelectedSensors(self):
		self.plotter_sensors.clear()
		if self.displayValueCheckBox.isChecked():
			for sensorIndice in range(len(self.selected_sensors)):
				self.plotter_sensors.plot(self.current_data[sensorIndice][:25], pen=pyqtgraph.mkPen(self.colors[sensorIndice], width=2), name=str('Sensor ' + str(self.selected_sensors[sensorIndice]) + ' value'))
				#print('lalalalalalalaladddd : ' + str(len(self.current_data[sensorIndice])))
		if self.displayProfileCheckBox.isChecked():
			for sensorIndice in range(len(self.selected_sensors)):
				self.plotter_sensors.plot(self.current_profiles[sensorIndice], pen=pyqtgraph.mkPen(self.colors[sensorIndice], width=2, style=Qt.DashLine), name=str('Sensor ' + str(self.selected_sensors[sensorIndice]) + ' profile'))
		if self.displayWeightCheckBox.isChecked():
			for sensorIndice in range(len(self.selected_sensors)):
				self.plotter_sensors.plot(self.current_weights[sensorIndice], pen=pyqtgraph.mkPen(self.colors[sensorIndice], width=2, style=Qt.DotLine), name=str('Sensor ' + str(self.selected_sensors[sensorIndice]) + ' weight'))
		
		# value : '-'
		# nominal value : '--'
		# weight : ':'

	""" Function that executes each time step """
	def tick(self, step=True):
		# Determine if there are new output files in the directory
		new_output_list = dict([(f, None) for f in os.listdir(self.output_path)])
		new_files = [f for f in new_output_list if not f in self.output_list]
		self.output_list = new_output_list

		if new_files:
			# Executes if there is a new file in the directory
			#new_output_list = dict([(f, None) for f in os.listdir(self.output_path)])
			#new_files = [f for f in new_output_list if not f in self.output_list]
			#self.output_list = new_output_list
			for file in new_files:
				# Load the data from each of the new files
				file_path = os.path.join(self.output_path, file)
				"""try:
					f = open(file_path, 'rb')
				except OSError:
					print ("Could not open/read file: "+ file_path)
					sys.exit()
				with f:
					print("====================== "+ str(file_path) )
					data = json.load(f)
				os.remove(file_path)"""
				isReady = False
				while not isReady:
					print ("try to read "+file_path)
					try:
						f = open(file_path, 'rb')
					except PermissionError:
						print ("Could not open/read file: "+ file_path)
						sys.exit()
					except ValueError:
						print ("Could not open/read file: "+ file_path)
						sys.exit()
					with f:
						print("====================== "+ str(file_path) )
						data = json.load(f)
					#os.remove(file_path)
						isReady = True
                
				"""while os.path.exists(file_path):
					with open(file_path, 'r') as f:
						print("====================== "+ str(file_path) )
						data = json.load(f)
					#print("====================== "+ str(data) )
					os.remove(file_path)"""


				list_string = data["data"]["timeStamp"] + " - " + data["data"]["anomalyState"]

				# Enter the new data into the decision list copy
				self.decision_list_copy.insertItem(0, list_string)
				self.decision_list_copy.item(0).setBackground(QColor("white"))

				# Check if the result was an ALERT
				if data["data"]["anomalyState"] == "ALERT":
					self.filtered_decision_list.insertItem(0, list_string)
					self.filtered_decision_list.item(0).setBackground(QColor("white"))

				# Check if currently filtering and add the appropriate item to the list
				if (self.filtering and data["data"]["anomalyState"] == "ALERT") or (not self.filtering):
					self.decision_list.insertItem(0, list_string)
					self.decision_list.item(0).setBackground(QColor("white"))

				# Add the new data to the display array
				
				#for i in range(len(self.data)):
				self.line_counter = self.columnTimestamps.index(data["data"]["timeStamp"])
				self.last_timestamp = data["data"]["timeStamp"]
				
				#if data["data"]["timeStamp"] in self.data[i]:
				#		self.line_counter = i
				#		print(self.data)
				#		print(data["data"]["timeStamp"])
				#		break
				for indexSensor in range(len(self.selected_sensors)):
					# Update the current data array
					self.current_data[indexSensor].append(float(self.data[self.line_counter][self.selected_sensors[indexSensor]]))
					self.current_data2[indexSensor].append(float(self.data[self.line_counter][self.selected_sensors[indexSensor]]))
					if len(self.current_data[indexSensor]) > 26:
						del(self.current_data[indexSensor][0])
				self.updateProfilesArrays(data["data"]["sensorsProfiles"])
				self.updateWeightsArrays(data["data"]["weights"])
				if len(self.anomaly_degrees) > 24:
					self.anomaly_degrees.pop(0)
				self.anomaly_degrees.append(float(data["data"]["anomalyDegree"]))
				self.anomaly_degrees2.append(float(data["data"]["anomalyDegree"]))
				if len(self.thresholds) > 24:
					self.thresholds.pop(0)
				self.thresholds.append(float(data["data"]["alertThreshold"]))
				self.totalExecutionTime = int(data["data"]["totalExecutionTime"])
				#print('Degré anomalie : ' + str(self.anomaly_degrees[23]))
				print(self.learning)
				if (self.last_timestamp == '2016-01-07T22:00') or (int(self.last_timestamp[9])> 7 or int(self.last_timestamp[8])> 0)  : # Start learning after 7 days
					self.learning = True
				# Giving feedback according to the feedback mode and time of previous feedback
				if self.feedbackMode != FeedbackMode.MANUAL and (self.last_timestamp == '2016-01-07T23:00' or (self.learning and self.lastFeedback == 'null')):
					self.validateDirectMode()
				elif self.feedbackMode == FeedbackMode.AUTOMATIC_HOUR and self.learning:
					self.validateDirectMode()
				elif self.feedbackMode == FeedbackMode.AUTOMATIC_DAY and self.columnTimestamps.index(self.last_timestamp) - self.columnTimestamps.index(self.lastFeedback) > (24)-1:
					self.validateDirectMode()
				elif self.feedbackMode == FeedbackMode.AUTOMATIC_WEEK and self.columnTimestamps.index(self.last_timestamp) - self.columnTimestamps.index(self.lastFeedback) > (24*7)-1:
					self.validateDirectMode()
			if step:
				# Plot the current data
				if self.started and not self.paused:
					self.plotAnomalyDegrees()
					self.plotSelectedSensors()
					
	""" Displays eht last 24 sliding anomaly degrees and the threshold (1000) """
	def plotAnomalyDegrees(self):
		pyqtgraph.setConfigOption('background', 'w')
		last_anomaly_degree_point = self.plotter_degrees.plot(x=[len(self.anomaly_degrees_points)], y=[self.anomaly_degrees[23]], pen=pyqtgraph.mkPen(255,255,255), symbol='o', symbolBrush=(0,0,255), symbolPen=(0,0,255), clickable=True)
		self.anomaly_degrees_points.append(last_anomaly_degree_point)
		self.anomaly_degrees_points[(len(self.anomaly_degrees_points)-1)].sigPointsClicked.connect(self.plotClicked)
		if len(self.anomaly_degrees_points) > 24:
			self.anomaly_degrees_points[len(self.anomaly_degrees_points) - 25].clear()
		self.plot_thresholds.clear()
		self.plot_thresholds = self.plotter_degrees.plot(x=list(range(len(self.anomaly_degrees_points)-25, len(self.anomaly_degrees_points))), y=self.thresholds, pen=pyqtgraph.mkPen((0,255,0), width=3))
		
	""" When an anomaly degree is clicked on the AD graph, the lines of the correspoding timestamp on the delision list and excel table view are selected """
	def plotClicked(self, curve):
		self.decision_list.item(0).setSelected(True)
		for item in self.decision_list.selectedItems():
			item.setSelected(False)
		self.decision_list.item(len(self.anomaly_degrees_points) -1 - self.anomaly_degrees_points.index(curve)).setSelected(True)
		timestamp_selected = self.decision_list.item(len(self.anomaly_degrees_points) -1 - self.anomaly_degrees_points.index(curve)).text()[0:16]
		self.table.selectRow(self.columnTimestamps.index(timestamp_selected) -1)

	""" Function to convert expert feedback to boolean isAnom """
	def isAnom(self, feedback, output):
		if feedback == "VALID":
			isAnom = (output == "ALERT")	
		elif feedback == "INCORRECT":
			isAnom = (output == "REGULAR")
		else:
			isAnom = False

		return isAnom

	""" Function to load data from a file """
	def loadFile(self, file_name):
		# Read in data
		with open(file_name, newline = '') as file:
			self.data = list(csv.reader(file))

			# Remove header row
			self.data = self.data[1:]

			# Load the new data into the table display
			self.table.updateFile(file_name)

			# Reset the counter, the decsion list and the figure's data points
			self.line_counter = 0
			self.decision_list.clear()
			self.selected_sensors = [1]
			self.current_data = [[0]*self.data_points]

	""" Function to write information to the status bar """
	def displayStatusMessage(self):
		text = "Current Value: " + str(self.data[self.line_counter][1]) + "	" + \
			str(Window.internalTimerSpeed) + "ms	 Index: " + str(self.line_counter)
		self.statusBar().showMessage(text)

	""" Bring up the dialog box to select CSV file to import """
	def importDialogCSV(self):
		options = QFileDialog.Options()
		options |= QFileDialog.DontUseNativeDialog
		fileName, _ = QFileDialog.getOpenFileName(self,"QFileDialog.getOpenFileName()", "","CSV Files (*.csv);;All Files (*)", options=options)

		# Refresh the simulation with the selected file
		if fileName:
			# Load the new file
			self.sensor = 1
			self.data_file = fileName
			self.loadFile(self.data_file)

			# Update the display
			self.tick(False)

	""" Bring up the dialog box to select save location for feedback record """
	def selectSaveDialog(self):
		file_path = str(QFileDialog.getExistingDirectory(self, "Select Directory"))

		os.remove(os.path.join(self.save_path, self.save_file)) # DELETES PREVIOUS SAVE FILE

		self.save_path = file_path

		with open(os.path.join(self.save_path, self.save_file), 'w') as save_file:
			writer = csv.writer(save_file)
			writer.writerow(["Time Stamp", "Feedback", "Sandman Output (isAnom)"])

	""" Function to write a file starting or restarting the simulation """
	def writeStartFile(self, timestamp):
		start_data = {}
		start_data["dataFile"] = self.data_file
		start_data["timeStamp"] = timestamp

		with open(os.path.join(self.input_path, self.input_file_start), 'w') as out:
			json.dump(start_data, out)

	""" Function to write a file playing or pausing the simulation """
	def writePlayFile(self, running=True):
		play_data = {}
		play_data["running"] = running
		play_data["dataFilePath"] = self.data_file

		with open(os.path.join(self.input_path, self.input_file_play), 'w') as out:
			json.dump(play_data, out)

	""" Function to write the file containing feedback for sandman """
	def writeFeedbackFile(self, feedback_array=None):
		feedback_data = {}
		feedback_data["arrayLength"] = len(feedback_array)
		feedback_data["feedbackArray"] = feedback_array

		with open(os.path.join(self.input_path, self.input_file_feedback), 'w') as out:
			json.dump(feedback_data, out)

	""" Function to write the feedback to data to a .csv file for reference """
	def writeSaveFile(self, feedbackArray=None):
		with open(os.path.join(self.save_path, self.save_file), 'a') as save_file:
			writer = csv.writer(save_file)

			for feedback in range(len(feedbackArray), 0, -1):
				data = []
				data.append(feedbackArray[feedback-1]["timeStamp"])
				data.append(feedbackArray[feedback-1]["expertFeedback"])
				data.append(feedbackArray[feedback-1]["isAnom"])

				writer.writerow(data)
				
	""" Function to change the state of the feedback mode """
	def changeFeedbackMode(self):
		indexCb = self.feedback_mode_combobox.currentIndex()
		if indexCb == 0:
			self.feedbackMode = FeedbackMode.MANUAL
		elif indexCb == 1:
			self.feedbackMode = FeedbackMode.AUTOMATIC_HOUR
		elif indexCb == 2:
			self.feedbackMode = FeedbackMode.AUTOMATIC_DAY
		elif indexCb == 3:
			self.feedbackMode = FeedbackMode.AUTOMATIC_WEEK
		print("Feedback mode changed to : " + str(self.feedbackMode))

	""" Function to write the new value of the timer """
	def writeTimerFile(self, newTimerValue):
		timer_data = {}
		timer_data["newTimerValue"] = newTimerValue
		with open(os.path.join(self.input_path, self.input_file_timer), 'w') as out:
			json.dump(timer_data, out)

	""" Clears input and output directories when starting or restarting sandman """
	def clearDirectories(self):
		files = glob.glob(os.path.join(self.output_path, '*'))
		for file in files:
			os.remove(file)

		files = glob.glob(os.path.join(self.input_path, '*'))
		for file in files:
			os.remove(file)
	
if __name__ == '__main__':
	app = QApplication(sys.argv)
	app.setStyle('Fusion')
	w = Window()
	sys.exit(app.exec_())
