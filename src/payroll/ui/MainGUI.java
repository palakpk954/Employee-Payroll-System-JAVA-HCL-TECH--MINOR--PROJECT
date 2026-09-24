package payroll.ui;

import payroll.exception.DuplicateEmployeeException;
import payroll.exception.EmployeeNotFoundException;
import payroll.exception.PayrollCalculationException;
import payroll.model.ContractEmployee;
import payroll.model.Employee;
import payroll.model.FullTimeEmployee;
import payroll.model.PartTimeEmployee;
import payroll.model.SalarySlip;
import payroll.repository.EmployeeRepository;
import payroll.service.PayrollProcessor;
import payroll.service.SalarySlipGenerator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

public class MainGUI extends JFrame {
    private EmployeeRepository<Employee> repository = new EmployeeRepository<>();
    private PayrollProcessor processor = new PayrollProcessor();
    private SalarySlipGenerator slipGenerator = new SalarySlipGenerator();
    
    private JTable employeeTable;
    private DefaultTableModel tableModel;

    public MainGUI() {
        setTitle("Employee Payroll Management System - UI");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        preloadData();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Top Header
        JLabel headerLabel = new JLabel("Employee Payroll Management", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(headerLabel, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"ID", "Name", "Department", "Type", "Join Date", "Net Salary"};
        tableModel = new DefaultTableModel(columns, 0);
        employeeTable = new JTable(tableModel);
        refreshTable();
        
        JScrollPane scrollPane = new JScrollPane(employeeTable);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Action Panel
        JPanel actionPanel = new JPanel(new FlowLayout());
        
        JButton btnAdd = new JButton("Add Full-Time Employee");
        JButton btnSlip = new JButton("View Selected Salary Slip");
        JButton btnPayroll = new JButton("Run Global Payroll");

        actionPanel.add(btnAdd);
        actionPanel.add(btnSlip);
        actionPanel.add(btnPayroll);
        add(actionPanel, BorderLayout.SOUTH);

        // Action Listeners
        btnAdd.addActionListener(e -> showAddEmployeeDialog());
        btnSlip.addActionListener(e -> showSalarySlip());
        btnPayroll.addActionListener(e -> runGlobalPayroll());
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Employee emp : repository.getAll()) {
            Object[] row = {
                    emp.getEmployeeId(),
                    emp.getName(),
                    emp.getDepartment(),
                    emp.getEmployeeType(),
                    emp.getJoiningDate().toString(),
                    String.format("₹ %.2f", emp.calculateNetSalary())
            };
            tableModel.addRow(row);
        }
    }

    private void showAddEmployeeDialog() {
        JTextField idField = new JTextField(10);
        JTextField nameField = new JTextField(10);
        JTextField deptField = new JTextField(10);
        JTextField basicField = new JTextField("40000", 10);

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.add(new JLabel("Employee ID:")); panel.add(idField);
        panel.add(new JLabel("Name:")); panel.add(nameField);
        panel.add(new JLabel("Department:")); panel.add(deptField);
        panel.add(new JLabel("Basic Salary:")); panel.add(basicField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Full-Time Employee", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                double basic = Double.parseDouble(basicField.getText());
                FullTimeEmployee ft = new FullTimeEmployee(
                        idField.getText(), nameField.getText(), deptField.getText(), 
                        "test@example.com", LocalDate.now(), 
                        basic, basic*0.2, basic*0.1, 2000, basic*0.12);
                
                repository.add(ft);
                refreshTable();
                JOptionPane.showMessageDialog(this, "Employee Added Successfully!");
            } catch (DuplicateEmployeeException ex) {
                JOptionPane.showMessageDialog(this, "ID already exists!", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid Data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showSalarySlip() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee from the table first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) tableModel.getValueAt(selectedRow, 0);
        try {
            Employee emp = repository.findById(id);
            SalarySlip slip = new SalarySlip("SLP-" + emp.getEmployeeId(), LocalDate.now().getMonth().toString(), emp);
            
            JTextArea textArea = new JTextArea(slip.format());
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            textArea.setEditable(false);
            
            JOptionPane.showMessageDialog(this, new JScrollPane(textArea), "Salary Slip", JOptionPane.INFORMATION_MESSAGE);
            
            // Optionally save to file
            slipGenerator.printAndSaveSlip(slip, true);
        } catch (EmployeeNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Employee not found.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runGlobalPayroll() {
        try {
            List<Employee> all = repository.getAll();
            double total = processor.calculateTotalPayrollWildcard(all);
            
            String message = String.format("Successfully ran payroll for %d employees.\n\nTotal Company Payout: ₹ %,.2f", all.size(), total);
            JOptionPane.showMessageDialog(this, message, "Global Payroll", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (PayrollCalculationException ex) {
            JOptionPane.showMessageDialog(this, "Error calculating payroll.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void preloadData() {
        try {
            repository.add(new FullTimeEmployee("EMP001", "Priya Sharma", "Engineering", "priya@example.com", LocalDate.of(2022, 1, 10), 40000, 8000, 4000, 3000, 4800));
            repository.add(new FullTimeEmployee("EMP002", "Rahul Gupta", "HR", "rahul@example.com", LocalDate.of(2021, 5, 15), 35000, 7000, 3500, 2000, 4200));
            repository.add(new PartTimeEmployee("EMP003", "Anil Kumar", "Support", "anil@example.com", LocalDate.of(2023, 2, 20), 200, 100));
            repository.add(new ContractEmployee("EMP005", "Vikram Singh", "Marketing", "vikram@example.com", LocalDate.of(2023, 1, 1), 600000, 12, LocalDate.of(2023, 12, 31), 5000));
        } catch (DuplicateEmployeeException ignored) {}
    }

    public static void main(String[] args) {
        // Set look and feel to system default for better aesthetics
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> {
            new MainGUI().setVisible(true);
        });
    }
}
