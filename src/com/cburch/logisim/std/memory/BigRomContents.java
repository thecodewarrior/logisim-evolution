/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.JFileChoosers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

class BigRomContents {
	BigInteger address = BigInteger.ZERO;
	Map<BigInteger, BigInteger> data = new HashMap<>();
	private File lastFile = null;

	BigInteger getCurrent() {
		BigInteger current = data.get(address);
		if(current == null) current = BigInteger.ZERO;
		return current;
	}

	public static BigRomContentsAttribute ATTR_CONTENTS = new BigRomContentsAttribute();
	static class BigRomContentsAttribute extends Attribute<BigRomContents> {

		public BigRomContentsAttribute() {
			super("contents", Strings.getter("bigRomContentsAttr"));
		}

		@Override
		public java.awt.Component getCellEditor(Window source, BigRomContents value) {
			ContentsCell ret = new ContentsCell(source, value);
			ret.mouseClicked(null);
			return ret;
		}

		@Override
		public String toDisplayString(BigRomContents value) {
			return Strings.get("bigRomContentsValue");
		}

		@Override
		public BigRomContents parse(String value) {
		    BigRomContents contents = new BigRomContents();
		    String[] entries = value.split("\\|");
			for (String entry : entries) {
				String[] keyValue = entry.split(":");
				contents.data.put(
						new BigInteger(keyValue[0], 16),
						new BigInteger(keyValue[1], 16)
				);
			}
			return contents;
		}

		@Override
		public String toStandardString(BigRomContents state) {
			StringBuilder text = null;
			for (Map.Entry<BigInteger, BigInteger> entry : state.data.entrySet()) {
				String key = entry.getKey().toString(16);
				String value = entry.getValue().toString(16);
				if (text == null)
					text = new StringBuilder(key + ":" + value);
				else
					text.append("|").append(key).append(":").append(value);
			}
			if(text == null) return "";
			return text.toString();
		}
	}

	public void open(File src) throws IOException {
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(src));
		} catch (IOException e) {
			throw new IOException(Strings.get("hexFileOpenError"), e);
		}
		try {
			String header = in.readLine();
			if (!(header.equals("v2.0 raw") || header.equals("v2.0 big"))) {
				throw new IOException(Strings.get("hexHeaderFormatError"));
			}
			open(in);
			try {
				BufferedReader oldIn = in;
				in = null;
				oldIn.close();
			} catch (IOException e) {
				throw new IOException(Strings.get("hexFileReadError"), e);
			}
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
			}
		}
	}

	private BigInteger origin = BigInteger.ZERO;
	private BigInteger offset = BigInteger.ZERO;

	private void open(BufferedReader in) throws IOException {
	    origin = BigInteger.ZERO;
		offset = BigInteger.ZERO;

		data.clear();
		String line;
		while((line = in.readLine()) != null) {
			line = line.split("#")[0].trim();
			if(line.length() == 0) continue;
			int index = line.indexOf(":");
			if(index != -1) {
				origin = new BigInteger(line.substring(0, index).trim(), 16);
				offset = BigInteger.ZERO;
				line = line.substring(index+1);
			}
			String[] elements = line.split(" ");
			for (String element : elements) {
				BigInteger repeat = BigInteger.ONE;
				index = element.indexOf("*");
				if(index != -1) {
					repeat = new BigInteger(element.substring(0, index).trim(), 10);
					element = element.substring(index+1);
				}
				BigInteger value = new BigInteger(element.trim(), 16);
				if(!value.equals(BigInteger.ZERO)) {
					for (BigInteger i = BigInteger.ZERO; i.compareTo(repeat) < 0; i = i.add(BigInteger.ONE)) {
						data.put(origin.add(offset).add(i), value);
					}
				}
				offset = offset.add(repeat);
			}
		}
	}

	@SuppressWarnings("serial")
	private static class ContentsCell extends JLabel implements MouseListener {
		Window source;
		BigRomContents contents;

		ContentsCell(Window source, BigRomContents contents) {
			super(Strings.get("romContentsValue"));
			this.source = source;
			this.contents = contents;
			addMouseListener(this);
		}

		public void mouseClicked(MouseEvent event) {
			if (contents == null)
				return;

			if(contents.lastFile == null) {
				LogisimFile logisimFile = source instanceof Frame ? ((Frame) source).getProject().getLogisimFile() : null;
				Loader loader = logisimFile == null ? null : logisimFile.getLoader();
				File mainFile = loader == null ? null : loader.getMainFile();
				if(mainFile != null) contents.lastFile = mainFile.getParentFile();
			}
			JFileChooser chooser = JFileChoosers.createSelected(contents.lastFile);
			chooser.setDialogTitle(Strings.get("openButton"));
			int choice = chooser.showOpenDialog(source);
			if (choice == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				try {
				    contents.open(f);
					contents.lastFile = f;
				} catch (IOException e) {
					JOptionPane.showMessageDialog(source,
							e.getMessage(),
							Strings.get("hexParseErrorTitle"),
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}
	}
}
