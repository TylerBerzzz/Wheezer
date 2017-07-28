function [d, deltaP_test, Q_test] = venturi(Q, deltaP, D, dtap, con_angle, div_angle)
%% Variables

iterations = 4;
tolerance = 0.001; % [1 thou]
font = 14;

%% Constants

Cd = 1;
rho = 1.2754; % [kg/m^3]
mu = 1.846e-5; % [kg/(m*s)]
nu = mu./rho; % [m^2/s]
% con_angle = 10.5; % convergence angle [degrees]
% div_angle = 7.5; % divergence angle [degrees]
Lleading = 1.75; % tube fitting fore [in]
Ltrailing = 0.25; % tube fitting aft [in]
Lthroat = 0.25; % throat length [in]


%% Solve for Throat Diameter

d = (1 / ( ( Cd*deltaP*pi^2/(8*Q^2*rho) ) + ( 1 / D^4 ) ) )^(1/4);

d_in = round(d/2.54e-2/tolerance)*tolerance;

Lcon = round((D-d)/2/tand(con_angle)/2.54e-2/tolerance)*tolerance; % x length of convergence section
Ldiv = round((D-d)/2/tand(div_angle)/2.54e-2/tolerance)*tolerance; % x length of divergence section


%% Draw the part
Lvector = [0, Lleading, Lcon, Lthroat, Ldiv, Ltrailing];
xPoints = cumsum(Lvector);
yPoints = [D/2, D/2, d/2, d/2, D/2, D/2]./2.54e-2;

figure
figurePosition(gcf,3,3,1)
plot(xPoints, yPoints,'k','linewidth',1.5)
hold on
plot(xPoints, -yPoints,'k','linewidth',1.5)

text(mean(xPoints(1:2)),0,['$D = ',num2str(D/2.54e-2),'$'],'interpreter','latex','fontsize',font);
text(xPoints(3),0,['$d = ',num2str(d_in),'$'],'interpreter','latex','fontsize',font);
text(mean(xPoints(1:4)),0,['$\theta_{con} = ',num2str(con_angle),'$'],'interpreter','latex','fontsize',font);
text(mean(xPoints(4:5)),0,['$\theta_{div} = ',num2str(div_angle),'$'],'interpreter','latex','fontsize',font);

text(0.1,2*D/2/2.54e-2,['$L_{lead} = ',num2str(Lleading),'$'],'interpreter','latex','fontsize',font);
text(xPoints(2),1.4*D/2/2.54e-2,['$L_{con} = ',num2str(Lcon),'$'],'interpreter','latex','fontsize',font);
text(xPoints(3),2*D/2/2.54e-2,['$L_{throat} = ',num2str(Lthroat),'$'],'interpreter','latex','fontsize',font);
text(xPoints(4),1.4*D/2/2.54e-2,['$L_{div} = ',num2str(Ldiv),'$'],'interpreter','latex','fontsize',font);
text(xPoints(5),2*D/2/2.54e-2,['$L_{tubefit} = ',num2str(Ltrailing),'$'],'interpreter','latex','fontsize',font);

text(mean(xPoints),-1.4*D/2/2.54e-2,['$L_{total} = ',num2str(sum(Lvector)),'$'],'interpreter','latex','fontsize',font);

axis('equal')
title('Inner Dimensions','interpreter','latex')

% set(gca,'interpreter','latex','fontsize',font)



%% Test case

d_test = d_in.*2.54e-2;

beta = d_test/D;
A0 = pi*d_test^2/4;
A1 = pi*D^2/4;
Dh = 4*A0./(pi*d_test); % Hydraulic diameter in a pipe

deltaP_test = linspace(0,deltaP,1000); % [Pa]
Q_test = A0*Cd*sqrt(2*deltaP_test/(rho*(1-beta^4))); % [m^3/s]

for ii = 1:iterations
    Re_d = Q_test*Dh./(nu.*A0);
    Re_star = dtap/d_test*Re_d;
    
    if Re_star > 60000
        Cd = 1.0011+0.0123*beta-0.0169*exp(-0.4*Re_star*1e-5);
    else
        Cd = 0.9878+0.0123*beta;
    end
    Q_test = A0*Cd*sqrt(2*deltaP_test/(rho*(1-beta^4)));
end

deltaPrange_inH2O = [deltaP_test(1) deltaP_test(end)]/622.1;
Qrange_lpm = [Q_test(1) Q_test(end)]*1e3*60;


figure
figurePosition(gcf,3,3,2)
plot(deltaP_test./249.1, Q_test*1e3*60,'linewidth',1.5)
grid on
ylabel('Q (\it{lpm})','fontname','Georgia')
xlabel('\DeltaP (\it{inH2O})','fontname','Georgia')
% set(gca,'interpreter','latex','fontsize',font)

end
