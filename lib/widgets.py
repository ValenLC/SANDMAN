'''
Created on 10 Feb 2020

@author: Anders Appel
'''

import csv

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (QSizePolicy, QListWidget, QMessageBox,
                            QAbstractItemView, QSlider, QGroupBox, QTableWidget,
                            QTableWidgetItem)

from matplotlib.backends.backend_qt5agg import FigureCanvasQTAgg as FigureCanvas
from matplotlib.figure import Figure
import matplotlib.pyplot as plt
from PyQt5.Qt import QTableWidget

class MatPlot(FigureCanvas):
    def __init__(self, parent=None, width=5, height=4, dpi=100):
        fig = Figure(figsize=(width, height), dpi=dpi)
        self.axes = fig.subplots()

        FigureCanvas.__init__(self, fig)
        self.setParent(parent)

        FigureCanvas.setSizePolicy(self, QSizePolicy.Expanding,
                                   QSizePolicy.Expanding)
        FigureCanvas.updateGeometry(self)

        # Create a palette of colours for plotting multiple lines
        self.colour_palette = ['r-',
                                'b-',
                                'k-',
                                'g-',
                                'y-',
                                'c-',
                                'm-']

    def plot(self, y_data, x_data=None, y_label='Data'):
        # Set display of the axes
        self.axes.clear()

        if x_data is None:
            self.axes.plot(y_data, 'r-', label=y_label)

        self.axes.set_title('Sensor Values in past 24 Hours')
        self.axes.legend(loc='upper right')

        self.draw()

    def plotMultipleSensors(self, y_data_array, y_labels=None):
        # Set display of the axes
        self.axes.clear()

        for i in range(len(y_data_array)):
            y_data = y_data_array[i]
            y_label = str(i) if y_labels == None else ("Sensor " + str(y_labels[i]))

            colour = self.colour_palette[i % len(self.colour_palette)]

            self.axes.plot(y_data, colour, label=y_label)

        self.axes.set_title('Sensor Values in past 24 Hours')
        self.axes.set_xlabel('Time (hours)')
        self.axes.legend(loc='upper right')

        self.draw()

    def plotHLine(self, y_value):
        self.axes.axhline(y=y_value, xmin=0, xmax=24)
        self.draw()

class CustomSlider(QSlider):
    def __init__(self, min, max, initial_value):
        QSlider.__init__(self, Qt.Horizontal)

        # Initialise the slider
        self.setMinimum(min)
        self.setMaximum(max)
        self.setValue(initial_value)

        self.setSingleStep(1)
        self.setTickInterval(500)
        self.setTickPosition(QSlider.TicksBelow)

class List(QListWidget):
    def __init__(self):
        super().__init__()
        self.setSelectionMode(QAbstractItemView.ExtendedSelection)

    def copyList(self, new_list):
        self.clear()
        for i in range(new_list.count()):
            item = new_list.item(i)
            self.addItem(item.text())

""" Custom table class to display data from .csv files """
class ExcelTable(QTableWidget):
    def __init__(self, file_path):
        super().__init__()

        self.file = file_path

        self.readFile()

    def readFile(self):
        # Read in the data
        with open(self.file, newline = '') as data_file:
            self.data = list(csv.reader(data_file))

        # If there is a row of headers
        headers = self.data[0]
        self.data = self.data[1:]

        self.setRowCount(len(self.data))
        self.setColumnCount(len(self.data[0]))

        self.setHorizontalHeaderLabels(headers)

        for row, columnvalues in enumerate(self.data):
            for column, value in enumerate(columnvalues):
                item = QTableWidgetItem(value)
                self.setItem(row, column, item)

    """ Function to update the data displayed with a new file """
    def updateFile(self, file_path):
        self.file = file_path
        self.readFile()
