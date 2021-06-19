import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

public class ClosestPair implements Runnable
{
    private Boolean running;
    private Display display;
    private ButtonsPanel buttons;
    private BufferStrategy bufferStrategy;
    private Graphics graphics;
    private Thread thread;
    private int width, height;

    private final int numOfPoints;
    private final Point[] points;
    private final ArrayList<StepData> stepData;
    private final Stack<int[]> pointersStack;
    private final int[][] processes;
    private int dataIndex;
    private int stepIndex;
    private int processIndex;
    private boolean play;
    private final double answer;

    public ClosestPair(Point[] points, ArrayList<StepData> stepData, double answer)
    {
        running = false;
        display = null;
        buttons = null;
        bufferStrategy = null;
        graphics = null;
        thread = null;

        numOfPoints = points.length;
        this.points = points;
        this.stepData = stepData;
        Collections.reverse(stepData);
        this.answer= answer;
        pointersStack = new Stack<>();
        stepIndex = 0;
        processIndex = 0;
        processes = new int[][]
                {
                        {2, 3, 4, 5}, // bruteforce 0
                        {7, 8}, // get mid point 1
                        {9, 10}, // divide array 2
                        {12}, // compute dl 3
                        {13}, // compute dr 4
                        {14}, // compute d 5
                        {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29}, // strip array 6
                        {31}, // strip closest p 7
                        {32}, // strip closest q 8
                        {33}, // compute minA 9
                        {35} // compute minB 10
                };
        play = false;
    }

    private void init()
    {
        display = new Display();
        width = display.getFrame().getWidth();
        height = display.getFrame().getHeight();
        display.getCodeSim().setLine(processes[processIndex]);

        buttons = display.getButtonsPanel();

        stepIndex = 0;
        processIndex = 0;
        play = false;
    }

    private void update()
    {
        CodeSim codeSim = display.getCodeSim();
        JScrollBar scrollBar = codeSim.getScrollPane().getVerticalScrollBar();

        if(pointersStack.empty() && processIndex >= processes.length - 1 && dataIndex >= stepData.size() - 1)
        {
            play = false;
            buttons.getRestartButton().setEnabled(true);
            buttons.getForwardButton().setEnabled(true);
            buttons.getSkipButton().setEnabled(true);
        }
        if(buttons.isPressed("Restart") && !play)
        {
            while(!pointersStack.empty())
            {
                pointersStack.pop();
            }
            dataIndex = 0;
            stepIndex = 0;
            processIndex = 0;
        }
        if(buttons.isPressed("Play"))
        {
            play = !play;
            buttons.getRestartButton().setEnabled(!play);
            buttons.getForwardButton().setEnabled(!play);
            buttons.getSkipButton().setEnabled(!play);
        }
        if((buttons.isPressed("Forward") && stepIndex < stepData.size()) || play)
        {

            if(!pointersStack.empty() && (processIndex >= processes.length - 1 || stepData.get(stepIndex).p.length <= 3))
            {
                int[] indices = pointersStack.pop();
                stepIndex = indices[0];
                processIndex = indices[1];
            }
            else if(processIndex < processes.length - 1)
            {
                if(processIndex == 3 || processIndex == 4)
                {
                    pointersStack.push(new int[]{stepIndex, processIndex + 1});
                    stepIndex = ++dataIndex;
                    processIndex = 0;
                }
                else
                {
                    processIndex++;
                }
            }
        }
        if(buttons.isPressed("Skip") && !play)
        {
            play = true;
            while(play)
            {
                if(pointersStack.empty() && processIndex >= processes.length - 1)
                {
                    play = false;
                }
                if(!pointersStack.empty() && (processIndex >= processes.length - 1 || stepData.get(stepIndex).p.length <= 3))
                {
                    int[] indices = pointersStack.pop();
                    stepIndex = indices[0];
                    processIndex = indices[1];
                }
                else if(processIndex < processes.length - 1)
                {
                    if(processIndex == 3 || processIndex == 4)
                    {
                        pointersStack.push(new int[]{stepIndex, processIndex + 1});
                        stepIndex = ++dataIndex;
                        processIndex = 0;
                    }
                    else
                    {
                        processIndex++;
                    }
                }
            }
        }

        // Sets scrollbar to follow process index
        if(buttons.isPressed() || play)
        {
            if(processIndex == 0 || processIndex == 5  || processIndex == 3 || processIndex == 4)
            {
                scrollBar.setValue(0);
            }
            else if(processIndex == processes.length || processIndex == 6 || processIndex == 10)
            {
                scrollBar.setValue(scrollBar.getMaximum());
            }
        }
    }

    private void render()
    {
        bufferStrategy = display.getCanvas().getBufferStrategy();
        if(bufferStrategy == null)
        {
            display.getCanvas().createBufferStrategy(2);
            return;
        }

        graphics = bufferStrategy.getDrawGraphics();
        graphics.clearRect(0, 0, width, height);
        graphics.setFont(new Font("Consolas", Font.PLAIN, 10));

        // START DRAW

        // Draw Code Sim Line Highlights
        if(buttons.isPressed() || play)
        {
            display.getCodeSim().resetLines();
            display.getCodeSim().setLine(processes[processIndex]); // highlights the lines
        }

        // Draw Points
        for(int i = 0; i < numOfPoints; i++)
        {
            graphics.drawOval(points[i].x - 2, points[i].y - 2, 4, 4);
        }

        // Draw Previous Array Bounds
        if(!pointersStack.empty())
        {
            StepData data = stepData.get(pointersStack.peek()[0]);
            Point lowPoint = data.leftPoints[0];
            Point highPoint = data.rightPoints[data.rightPoints.length - 1];
            Point middlePoint = data.middlePoint;
            graphics.setColor(Color.MAGENTA);
            graphics.drawLine(lowPoint.x, 0, lowPoint.x, height);
            graphics.drawLine(highPoint.x, 0, highPoint.x, height);
        }

        StepData data = stepData.get(stepIndex);
        // Draw Step's Middle Point
        if(data.middlePoint != null)
        {
            Point middlePoint = data.middlePoint;
            Point lowPoint = data.leftPoints[0];
            Point highPoint = data.rightPoints[data.rightPoints.length - 1];
            switch(processIndex)
            {
                case 11:
                case 10:
                    graphics.setColor(Color.ORANGE);
                    graphics.drawString("Min: " + Math.round(data.min), middlePoint.x, 100);
                case 9:
                    graphics.setColor(Color.YELLOW);
                    if(data.stripQ != null && data.stripQ.length != 0)
                    {
                        graphics.drawString("Min B: " + Math.round(data.minB), data.stripQ[data.stripQ.length / 2].x, data.stripQ[data.stripQ.length / 2].y);
                    }
                case 8:
                    graphics.setColor(Color.GREEN);
                    if(data.stripP != null && data.stripP.length != 0)
                    {
                        graphics.drawString("Min A: " + Math.round(data.minA), data.stripP[data.stripP.length / 2].x, data.stripP[data.stripP.length / 2].y);
                    }
                case 7:
                    graphics.setColor(Color.GREEN);
                    for(Point p : data.stripP)
                    {
                        graphics.drawOval(p.x - 2, p.y - 2, 4, 4);

                    }
                    graphics.setColor(Color.YELLOW);
                    for(Point q : data.stripQ)
                    {
                        graphics.drawOval(q.x - 2, q.y - 2, 4, 4);
                    }
                case 6:
                    graphics.setColor(Color.BLACK);
                    graphics.drawString("Min: " + Math.round(data.d), middlePoint.x, height / 2);
                case 5:
                    graphics.setColor(Color.RED);
                    graphics.drawString("Left Min: " + Math.round(data.dl), lowPoint.x, height / 3);
                    graphics.setColor(Color.BLUE);
                    graphics.drawString("Right Min: " + Math.round(data.dr), middlePoint.x, (int) (height / 1.5));
                case 3:
                    graphics.setColor(Color.RED);
                    graphics.drawLine(lowPoint.x, 0, lowPoint.x, height);
                    graphics.drawLine(lowPoint.x, height / 2, middlePoint.x, height /2);
                    graphics.setColor(Color.BLUE);
                    graphics.drawLine(highPoint.x, 0, highPoint.x, height);
                    graphics.drawLine(highPoint.x, height / 2, middlePoint.x, height /2);
                case 2:
                    graphics.setColor(Color.BLACK);
                    graphics.drawLine(middlePoint.x, 0, middlePoint.x, height);
            }
        }

        // Print Final Answer
        if(pointersStack.empty() && dataIndex >= stepData.size() - 1 && processIndex >= processes.length - 1)
        {
            graphics.setFont(new Font("Consolas", Font.BOLD, 20));
            graphics.setColor(Color.BLACK);
            graphics.drawString("Answer:" + answer, 10, 20);
        }

        // END DRAW

        bufferStrategy.show();
        graphics.dispose();
    }

    @Override
    public void run()
    {
        init();

        int fps = 30;
        double timePerTick = 1000000000 / (double) fps;
        double delta = 0;
        long now;
        long lastTime = System.nanoTime();
        long timer = 0;
        while(running)
        {
            now = System.nanoTime();
            delta += (now - lastTime) / timePerTick;
            timer += now - lastTime;
            lastTime = now;
            if(delta >= 1)
            {
                update();
                render();
                buttons.resetButtons();
                delta--;
            }
            if(timer >= 1000000000)
            {
                timer = 0;
            }
        }
    }

    public synchronized void start()
    {
        if(running)
        {
            return;
        }
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void stop()
    {
        if(!running)
        {
            return;
        }
        try
        {
            thread.join();
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
