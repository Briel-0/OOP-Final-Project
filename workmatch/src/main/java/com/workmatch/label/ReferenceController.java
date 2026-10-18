package com.workmatch.label;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workmatch.Label;
import com.workmatch.LabelRepository;
import com.workmatch.Location;
import com.workmatch.LocationRepository;

import java.util.List;

@RestController
public class ReferenceController {

    private final LabelRepository labelRepository;
    private final LocationRepository locationRepository;

    public ReferenceController(LabelRepository labelRepository, LocationRepository locationRepository) {
        this.labelRepository = labelRepository;
        this.locationRepository = locationRepository;
    }

    @GetMapping("/api/labels")
    public List<Label> getAllLabels() {
        return labelRepository.findAll();
    }

    @GetMapping("/api/locations")
    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }
}