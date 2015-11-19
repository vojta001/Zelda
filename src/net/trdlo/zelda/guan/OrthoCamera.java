package net.trdlo.zelda.guan;

import net.trdlo.zelda.XY;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.trdlo.zelda.ZeldaFrame;

class OrthoCamera {

	public static final double ZOOM_BASE = 1.1;
	private static final Stroke defaultStroke = new BasicStroke(1);
	private static final Stroke selectionStroke = new BasicStroke(2);
	private static final Stroke dashStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);

	private boolean boundsDebug = false;

	private World world;
	private double x, y;
	private int zoom;

	private double zoomCoefLimit;
	private Rectangle componentBounds, cameraBounds;

	private Point dragStart, dragEnd, moveStart, moveEnd;
	private final Set<Point> selection;
	private Set<Point> tempSelection;
	private boolean additiveSelection;
	//private Set<Line> selectedLines; 

	public OrthoCamera(World world, double x, double y, int zoom) {
		setWorld(world, x, y, zoom);

		selection = new HashSet<>();
		tempSelection = new HashSet<>();
	}

	public final void setWorld(World world, double x, double y, int zoom) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.zoom = zoom;
	}

	public void update() {

	}

	private int worldToViewX(double x) {
		assert componentBounds != null;
		return (int) ((componentBounds.width / 2) + (x - this.x) * zoomCoef());
	}

	public int worldToViewY(double y) {
		assert componentBounds != null;
		return (int) ((componentBounds.height / 2) + (y - this.y) * zoomCoef());
	}

	public XY worldToView(Point p) {
		return new XY(worldToViewX(p.x), worldToViewY(p.y));
	}

	public double viewToWorldX(int x) {
		assert componentBounds != null;
		return (x - (componentBounds.width / 2)) / zoomCoef() + this.x;
	}

	public double viewToWorldY(int y) {
		assert componentBounds != null;
		return (y - (componentBounds.height / 2)) / zoomCoef() + this.y;
	}

	public Point viewToWorld(XY xy) {
		return new Point(viewToWorldX(xy.x), viewToWorldY(xy.y));
	}

	private void renderBoundsDebug(Graphics2D graphics) {
		if (world.bounds == null) {
			graphics.setColor(Color.RED);
			graphics.drawString("Bounds not set!", 50, 10);
			graphics.setColor(Color.WHITE);
			return;
		}

		graphics.setStroke(selectionStroke);
		graphics.setColor(Color.PINK);
		int l = worldToViewX(world.bounds.x), t = worldToViewY(world.bounds.y), w = worldToViewX(world.bounds.x + world.bounds.width) - l, h = worldToViewY(world.bounds.y + world.bounds.height) - t;
		graphics.drawRect(l, t, w, h);
		if (cameraBounds != null) {
			graphics.setStroke(defaultStroke);
			graphics.setColor(Color.RED);
			graphics.drawRect(worldToViewX(x) - Point.DISPLAY_SIZE / 2, worldToViewY(y) - Point.DISPLAY_SIZE / 2, Point.DISPLAY_SIZE, Point.DISPLAY_SIZE);
			l = worldToViewX(cameraBounds.x);
			t = worldToViewY(cameraBounds.y);
			w = worldToViewX(cameraBounds.x + cameraBounds.width) - l;
			h = worldToViewY(cameraBounds.y + cameraBounds.height) - t;
			if (w > 0 && h > 0) {
				graphics.drawRect(l, t, w, h);
			} else if (w > 0) {
				t = worldToViewY(cameraBounds.y + cameraBounds.height / 2);
				graphics.drawLine(l, t, l + w, t);
			} else if (h > 0) {
				l = worldToViewX(cameraBounds.x + cameraBounds.width / 2);
				graphics.drawLine(l, t, l, t + h);
			}

		}
	}

	private void renderReflectionsDebug(Graphics2D graphics) {
		if (world.lines.size() < 2) {
			return;
		}
		Iterator<Line> it = world.lines.iterator();
		Line t = it.next(), m = it.next();

		Line r = m.reflect(t);
		graphics.drawLine(worldToViewX(r.A.x), worldToViewY(r.A.y), worldToViewX(r.B.x), worldToViewY(r.B.y));
		Line b = m.bounceOff(t);
		if (b != null) {
			graphics.drawLine(worldToViewX(b.A.x), worldToViewY(b.A.y), worldToViewX(b.B.x), worldToViewY(b.B.y));
		}
		Line br = m.bounceOffRay(t);
		if (br != null) {
			graphics.drawLine(worldToViewX(br.A.x), worldToViewY(br.A.y), worldToViewX(br.B.x), worldToViewY(br.B.y));
		}
	}

	private void renderDistanceDebug(Graphics2D graphics) {
		if (world.lines.size() < 1) {
			return;
		}

		Iterator<Line> it = world.lines.iterator();
		Line l = it.next();

		XY mxy = ZeldaFrame.getInstance().getMouseXY();
		Point p = viewToWorld(mxy);
		double d = l.getSegmentDistance(p);
		graphics.drawString(String.format("d=%.2f", d), mxy.x, mxy.y);
	}

	private void renderProximityAndCrossingDebug(Graphics2D graphics, Line line) {
		XY mxy = ZeldaFrame.getInstance().getMouseXY();
		Point p = viewToWorld(mxy);

		if (line.getSegmentDistance(p) < 10 / zoomCoef()) {
			graphics.setColor(Color.PINK);
		} else {
			graphics.setColor(Color.WHITE);
		}

		graphics.setStroke(defaultStroke);
		for (Line cross : world.lines) {
			if (cross != line && cross.segmentIntersectsSegment(line)) {
				graphics.setStroke(dashStroke);
				break;
			}
		}
	}

	public void render(Graphics2D graphics, Rectangle componentBounds, float renderFraction) {
		this.componentBounds = componentBounds;

		if (boundsDebug) {
			renderBoundsDebug(graphics);
		}

		double dx = 0, dy = 0;
		if (moveStart != null && moveEnd != null) {
			dx = moveEnd.getX() - moveStart.getX();
			dy = moveEnd.getY() - moveStart.getY();
		}

		graphics.setStroke(defaultStroke);
		graphics.setColor(Color.WHITE);
		for (Line line : world.lines) {
			double lAx = line.getA().x, lAy = line.getA().y;
			double lBx = line.getB().x, lBy = line.getB().y;
			if (selection.contains(line.getA())) {
				lAx += dx;
				lAy += dy;
			}
			if (selection.contains(line.getB())) {
				lBx += dx;
				lBy += dy;
			}

			renderProximityAndCrossingDebug(graphics, line);

			graphics.drawLine(worldToViewX(lAx), worldToViewY(lAy), worldToViewX(lBx), worldToViewY(lBy));
		}

		for (Point point : world.points) {
			double px = point.x, py = point.y;
			if (selection.contains(point) || tempSelection.contains(point)) {
				graphics.setStroke(selectionStroke);
				graphics.setColor(Color.PINK);
				px += dx;
				py += dy;
			} else {
				graphics.setStroke(defaultStroke);
				graphics.setColor(Color.WHITE);
			}
			int vx = worldToViewX(px);
			int vy = worldToViewY(py);
			graphics.drawRect(vx - Point.DISPLAY_SIZE / 2, vy - Point.DISPLAY_SIZE / 2, Point.DISPLAY_SIZE, Point.DISPLAY_SIZE);
			graphics.drawString(point.getDescription(), vx + Point.DISPLAY_SIZE, vy + Point.DISPLAY_SIZE);
		}

		renderReflectionsDebug(graphics);
		renderDistanceDebug(graphics);

		if (dragStart != null && dragEnd != null) {
			graphics.setStroke(dashStroke);
			graphics.setColor(Color.LIGHT_GRAY);
			graphics.drawRect(
					worldToViewX(Math.min(dragStart.getX(), dragEnd.getX())),
					worldToViewY(Math.min(dragStart.getY(), dragEnd.getY())),
					(int) (Math.abs(dragStart.getX() - dragEnd.getX()) * zoomCoef()),
					(int) (Math.abs(dragStart.getY() - dragEnd.getY()) * zoomCoef()));
		}
	}

	private double zoomCoef() {
		double zoomCoef = Math.pow(ZOOM_BASE, zoom);
		return (zoomCoefLimit > zoomCoef) ? zoomCoefLimit : zoomCoef;
	}

	public void zoom(int change, XY fixedPoint) {
		if (componentBounds == null) {
			return;
		}

		double wx = x;
		double wy = y;
		if (fixedPoint != null) {
			wx = viewToWorldX(fixedPoint.x);
			wy = viewToWorldY(fixedPoint.y);
		}

		double c1 = zoomCoef();
		if (zoomCoefLimit < c1 || change > 0) {
			zoom += change;
		}
		double coefChange = zoomCoef() / c1;

		double dx = (wx - x) / coefChange;
		double dy = (wy - y) / coefChange;

		x = wx - dx;
		y = wy - dy;
		checkBounds();
	}

	public void move(XY diff) {
		x += diff.x / zoomCoef();
		y += diff.y / zoomCoef();
		checkBounds();
	}

	public void checkBounds() {
		if (world.bounds == null) {
			return;
		}

		zoomCoefLimit = Math.min(componentBounds.width / (double) world.bounds.width, componentBounds.height / (double) world.bounds.height);

		double left = world.bounds.x + (componentBounds.width / 2) / zoomCoef();
		double right = world.bounds.x + world.bounds.width - (componentBounds.width / 2) / zoomCoef();
		double top = world.bounds.y + (componentBounds.height / 2) / zoomCoef();
		double bottom = world.bounds.y + world.bounds.height - (componentBounds.height / 2) / zoomCoef();

		cameraBounds = new Rectangle((int) left, (int) top, (int) (right - left), (int) (bottom - top));

		if (left < right) {
			if (x < left) {
				x = left;
			}
			if (x > right) {
				x = right;
			}
		} else {
			x = world.bounds.x + world.bounds.width / 2;
		}
		if (top < bottom) {
			if (y < top) {
				y = top;
			}
			if (y > bottom) {
				y = bottom;
			}
		} else {
			y = world.bounds.y + world.bounds.height / 2;
		}
	}

	public boolean isBoundsDebug() {
		return boundsDebug;
	}

	public void setBoundsDebug(boolean showBoundsDebug) {
		this.boundsDebug = showBoundsDebug;
	}

	public Point getPointAt(int x, int y) {
		return world.getPointAt(viewToWorldX(x), viewToWorldY(y), Point.DISPLAY_SIZE / zoomCoef());
	}

	public Set<Point> getPointsIn(int x1, int y1, int x2, int y2) {
		return world.getPointsIn(viewToWorldX(x1), viewToWorldY(y1), viewToWorldX(x2), viewToWorldY(y2));
	}

	public void mouse1pressed(MouseEvent e) {
		additiveSelection = e.isShiftDown();
		Point pointAt = getPointAt(e.getX(), e.getY());
		if (pointAt != null) {
			if (!additiveSelection && !selection.contains(pointAt)) {
				selection.clear();
			}
			selection.add(pointAt);
			moveStart = new Point(viewToWorldX(e.getX()), viewToWorldY(e.getY()));
		} else {
			dragStart = new Point(viewToWorldX(e.getX()), viewToWorldY(e.getY()));
			//assert dragEnd == null;
		}
	}

	public void mouse1dragged(MouseEvent e) {
		if (dragStart != null) {
			if (dragEnd == null) {
				dragEnd = new Point();
			}
			dragEnd.setX(viewToWorldX(e.getX()));
			dragEnd.setY(viewToWorldY(e.getY()));

			tempSelection = world.getPointsIn(dragStart.getX(), dragStart.getY(), dragEnd.getX(), dragEnd.getY());
			if (!additiveSelection) {
				selection.clear();
			}
		} else if (moveStart != null) {
			if (moveEnd == null) {
				moveEnd = new Point();
			}
			moveEnd.setX(viewToWorldX(e.getX()));
			moveEnd.setY(viewToWorldY(e.getY()));
		}
	}

	public void mouse1released(MouseEvent e) {
		if (dragStart != null) {
			if (!additiveSelection) {
				selection.clear();
			}
			if (dragEnd != null) {
				selection.addAll(tempSelection);
				tempSelection.clear();
				dragEnd = null;
			}
			dragStart = null;
		} else if (moveStart != null && moveEnd != null) {
			world.shiftPoints(selection, moveEnd.getX() - moveStart.getX(), moveEnd.getY() - moveStart.getY());
			moveStart = moveEnd = null;
		}
	}

}
