#include "splinelib.h"

#include <stdlib.h>

int trajectory_prepare_candidate(Waypoint *path, int path_length, void (*fit)(Waypoint,Waypoint,Spline*), double dt,
        double max_velocity, double max_acceleration, double max_jerk, TrajectoryCandidate *cand) {
    if (path_length < 2) return -1;
    
    Spline *splines = malloc((path_length - 1) * sizeof(Spline));
    double *splineLengths = malloc((path_length - 1) * sizeof(double));
    double totalLength = 0;
    
    int i;
    for (i = 0; i < path_length-1; i++) {
        Spline s;
        fit(path[i], path[i+1], &s);
        double dist = spline_distance(&s);
        splines[i] = s;
        splineLengths[i] = dist;
        totalLength += dist;
    }
    
    TrajectoryConfig config = {dt, max_velocity, max_acceleration, max_jerk, 0, path[0].angle,
        totalLength, 0, path[0].angle};
    TrajectoryInfo info = trajectory_prepare(config);
    int trajectory_length = info.length;
    
    cand->saptr = &splines;
    cand->laptr = &splineLengths;
    cand->totalLength = totalLength;
    cand->length = trajectory_length;
    cand->path_length = path_length;
    cand->info = info;
    cand->config = config;
    
    return 0;
}

int trajectory_generate(TrajectoryCandidate *c, Segment *segments) {
    int trajectory_length = c->length;
    int path_length = c->path_length;
    double totalLength = c->totalLength;
    
    Spline *splines = *(c->saptr);
    double *splineLengths = *(c->laptr);
    
    trajectory_create(c->info, c->config, segments);
    
    int spline_i = 0;
    double spline_pos_initial, splines_complete;
    
    int i;
    for (i = 0; i < trajectory_length; ++i) {
        double pos = segments[i].position;

        int found = 0;
        while (!found) {
            double pos_relative = pos - spline_pos_initial;
            if (pos_relative <= splineLengths[spline_i]) {
                Spline si = splines[spline_i];
                double percentage = spline_progress_for_distance(si, pos_relative);
                Coord coords = spline_coords(si, percentage);
                segments[i].heading = spline_angle(si, percentage);
                segments[i].x = coords.x;
                segments[i].y = coords.y;
                found = 1;
            } else if (spline_i < path_length - 2) {
                splines_complete += splineLengths[spline_i];
                spline_pos_initial = splines_complete;
                spline_i += 1;
            } else {
                Spline si = splines[path_length - 2];
                segments[i].heading = spline_angle(si, 1.0);
                Coord coords = spline_coords(si, 1.0);
                segments[i].x = coords.x;
                segments[i].y = coords.y;
                found = 1;
            }
        }
    }
    
    return trajectory_length;
}