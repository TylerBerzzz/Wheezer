%% Venturi design
% This code designs Venturi dimensions given the design inputs of max flow
% (Qmax), pressure gauge limit (deltaP), major diameter (D), tap diameter
% (dtap), convergence angle (theta_con), and divergence angle (theta_div)



close all; clear all; clc;

SAVE = 0;
set(0, 'DefaultAxesBox', 'on');
font = 14;

%% Inputs

Qmax = 10e-3; % [m^3/s]
D = 0.875*2.54e-2; % [m]
% deltaP = 10*(249.1); % [inH2O to Pa]
deltaP = 5*(6894.76); % [psi to Pa]
dtap = 0.120*2.54e-2; % [m]
theta_con = 10.5; % [degrees]
theta_div = 7.5; %  [degrees]

%% Call function

savename = ['Q' num2str(Qmax*60*1e3)];

[d, deltaP, Q] = venturi(Qmax,deltaP,D,dtap,theta_con,theta_div);

if SAVE == 1
    export_fig(gcf,['VenturiCurve_', savename],'-r600','-png','-transparent');
    export_fig(gcf,['VenturiDims_', savename],'-r600','-png','-transparent')
end

setraV = deltaP/249.1*2;
Qlpm = Q*60*1e3;



