package dev.wildcatter.tailings.haul;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A locomotive and up to 10 cars. An eleventh car does not couple. */
public final class Train {
	private final String id;
	private final List<CarLoad> cars = new ArrayList<>();

	public Train(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public int cars() {
		return cars.size();
	}

	public boolean couple(CarLoad car) {
		if (car == null || cars.size() >= Haul.MAX_CARS) {
			return false;
		}
		cars.add(car);
		return true;
	}

	public List<CarLoad> loads() {
		return Collections.unmodifiableList(cars);
	}
}
